// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.modules.decompiler;

import org.jetbrains.java.decompiler.code.CodeConstants;
import org.jetbrains.java.decompiler.main.ClassesProcessor;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.main.rels.ClassWrapper;
import org.jetbrains.java.decompiler.main.rels.MethodWrapper;
import org.jetbrains.java.decompiler.modules.decompiler.exps.*;
import org.jetbrains.java.decompiler.modules.decompiler.exps.FunctionExprent.FunctionType;
import org.jetbrains.java.decompiler.modules.decompiler.stats.*;
import org.jetbrains.java.decompiler.struct.StructField;
import org.jetbrains.java.decompiler.struct.StructMethod;
import org.jetbrains.java.decompiler.struct.gen.VarType;
import org.jetbrains.java.decompiler.util.DotExporter;
import org.jetbrains.java.decompiler.util.Pair;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class SwitchHelper {
  public static boolean simplifySwitches(Statement stat, StructMethod mt, RootStatement root) {
    boolean ret = false;
    if (stat instanceof SwitchStatement) {
      ret = simplify((SwitchStatement)stat, mt, root);
    }

    for (int i = 0; i < stat.getStats().size(); i++) {
      ret |= simplifySwitches(stat.getStats().get(i), mt, root);
    }

    return ret;
  }

  private static boolean simplify(SwitchStatement switchStatement, StructMethod mt, RootStatement root) {
    SwitchHeadExprent switchHeadExprent = (SwitchHeadExprent)switchStatement.getHeadexprent();
    Exprent value = switchHeadExprent.getValue();
    ArrayExprent array = getEnumArrayExprent(value, root);
    if (array != null) {
      List<List<Exprent>> caseValues = switchStatement.getCaseValues();
      Map<Exprent, Exprent> mapping = new HashMap<>(caseValues.size());
      if (array.getArray() instanceof FieldExprent) {
        FieldExprent arrayField = (FieldExprent) array.getArray();
        ClassesProcessor.ClassNode classNode = DecompilerContext.getClassProcessor().getMapRootClasses().get(arrayField.getClassname());
        if (classNode != null) {
          ClassWrapper classWrapper = classNode.getWrapper();
          if (classWrapper != null) {
            MethodWrapper wrapper = classWrapper.getMethodWrapper(CodeConstants.CLINIT_NAME, "()V");
            if (wrapper != null && wrapper.root != null) {
              // The enum array field's assignments if the field is built with a temporary local variable.
              // We need this to find the array field's values from the container class.
              List<AssignmentExprent> fieldAssignments = getAssignmentsOfWithinOneStatement(wrapper.root, arrayField);
              // If assigned more than once => not what we're looking for and discard the list
              if (fieldAssignments.size() > 1) {
                fieldAssignments.clear();
              }

              // Keep track of whether the assignment of the array field has already happened.
              // The same local variable might be used for multiple arrays (like with Kotlin, for example.)
              boolean[] fieldAssignmentEncountered = new boolean[] { false }; // single-element reference for lambdas

              wrapper.getOrBuildGraph().iterateExprents(exprent -> {
                if (exprent instanceof AssignmentExprent) {
                  AssignmentExprent assignment = (AssignmentExprent) exprent;
                  Exprent left = assignment.getLeft();
                  if (left instanceof ArrayExprent) {
                    Exprent assignmentArray = ((ArrayExprent) left).getArray();
                    // If the assignment target is a field, we have the assignment we want.
                    boolean targetsField = assignmentArray.equals(arrayField);

                    // If the target is a local variable, this gets more complicated.
                    // Kotlin (as mentioned above) creates its enum arrays by storing the array
                    // in a local first, so we need to check if the variable is later uniquely
                    // assigned to the enum array.
                    if (!targetsField && assignmentArray instanceof VarExprent && !fieldAssignmentEncountered[0]) {
                      for (AssignmentExprent fieldAssignment : fieldAssignments) {
                        if (fieldAssignment.getRight().equals(assignmentArray)) {
                          targetsField = true;
                          break;
                        }
                      }
                    }

                    if (targetsField) {
                      mapping.put(assignment.getRight(), ((InvocationExprent) ((ArrayExprent) left).getIndex()).getInstance());
                    }
                  } else if (fieldAssignments.contains(exprent)) {
                    fieldAssignmentEncountered[0] = true;
                  }
                }
                return 0;
              });
            }
          }
        }
      } else { // Invocation
        InvocationExprent invocation = (InvocationExprent) array.getArray();
        ClassesProcessor.ClassNode classNode = DecompilerContext.getClassProcessor().getMapRootClasses().get(invocation.getClassname());
        if (classNode != null) {
          ClassWrapper classWrapper = classNode.getWrapper();
          if (classWrapper != null) {
            MethodWrapper wrapper = classWrapper.getMethodWrapper(invocation.getName(), "()[I");
            if (wrapper != null && wrapper.root != null) {
              wrapper.getOrBuildGraph().iterateExprents(exprent -> {
                if (exprent instanceof AssignmentExprent) {
                  AssignmentExprent assignment = (AssignmentExprent) exprent;
                  Exprent left = assignment.getLeft();
                  if (left instanceof ArrayExprent) {
                    mapping.put(assignment.getRight(), ((InvocationExprent) ((ArrayExprent) left).getIndex()).getInstance());
                  }
                }
                return 0;
              });
            }
          } else {
            // Need to wait til last minute processing
            return false;
          }
        }
      }

      List<List<Exprent>> realCaseValues = new ArrayList<>(caseValues.size());
      for (List<Exprent> caseValue : caseValues) {
        List<Exprent> values = new ArrayList<>(caseValue.size());
        realCaseValues.add(values);
        cases:
        for (Exprent exprent : caseValue) {
          if (exprent == null) {
            values.add(null);
          }
          else {
            Exprent realConst = mapping.get(exprent);
            if (realConst == null) {
              if (exprent instanceof ConstExprent) {
                ConstExprent constLabel = (ConstExprent) exprent;
                if (constLabel.getConstType().typeFamily == CodeConstants.TYPE_FAMILY_INTEGER) {
                  int intLabel = constLabel.getIntValue();
                  // check for -1, used by nullable switches for the null branch
                  if (intLabel == -1) {
                    values.add(new ConstExprent(VarType.VARTYPE_NULL, null, null));
                    continue;
                  }
                  // other values can show up in a `tableswitch`, such as in [-1, fall-through synthetic 0, 1, 2, ...]
                  // they must have a valid value later though
                  // TODO: more tests
                  for (Exprent key : mapping.keySet()) {
                    if (key instanceof ConstExprent
                        && ((ConstExprent) key).getConstType().typeFamily == CodeConstants.TYPE_FAMILY_INTEGER
                        && ((ConstExprent) key).getIntValue() > intLabel) {
                      values.add(key.copy());
                      continue cases;
                    }
                  }
                }
              }
              root.addComment("$VF: Unable to simplify switch on enum", true);
              DecompilerContext.getLogger()
                .writeMessage("Unable to simplify switch on enum: " + exprent + " not found, available: " + mapping + " in method " + mt.getClassQualifiedName() + " " + mt.getName(),
                              IFernflowerLogger.Severity.ERROR);
              return false;
            }
            values.add(realConst.copy());
          }
        }
      }
      caseValues.clear();
      caseValues.addAll(realCaseValues);
      Exprent newExpr = ((InvocationExprent)array.getIndex()).getInstance().copy();
      switchHeadExprent.replaceExprent(value, newExpr);
      newExpr.addBytecodeOffsets(value.bytecode);

      // If we replaced the only use of the local var, the variable should be removed altogether.
      if (value instanceof VarExprent) {
        VarExprent var = (VarExprent) value;
        List<Pair<Statement, Exprent>> references = new ArrayList<>();
        findExprents(root, Exprent.class, var::isVarReferenced, false, (stat, expr) -> references.add(Pair.of(stat, expr)));

        // If we only have one reference...
        if (references.size() == 1) {
          // ...and if it's just an assignment, remove it.
          Pair<Statement, Exprent> ref = references.get(0);
          if (ref.b instanceof AssignmentExprent && ((AssignmentExprent) ref.b).getLeft().equals(value)) {
            ref.a.getExprents().remove(ref.b);
          }
        }
      }

      return true;
    } else if (isSwitchOnString(switchStatement)) {
      Map<Integer, Exprent> caseMap = new HashMap<>();

      SwitchStatement following;
      boolean nullable = false;
      IfStatement containingNullCheck = null;
      List<StatEdge> edges = switchStatement.getSuccessorEdges(StatEdge.TYPE_REGULAR);
      if (edges.size() == 1 && edges.get(0).getDestination() instanceof SwitchStatement) {
        following = (SwitchStatement)edges.get(0).getDestination();
      } else {
        // definitely a nullable switch
        // we've already validated the `if` in `isSwitchOnString`
        nullable = true;
        containingNullCheck = (IfStatement) switchStatement.getParent();
        following = (SwitchStatement) containingNullCheck.getSuccessorEdges(StatEdge.TYPE_REGULAR).get(0).getDestination();
      }

      int i = 0;
      for (; i < switchStatement.getCaseStatements().size(); ++i) {

        Statement curr = switchStatement.getCaseStatements().get(i);

        while (curr instanceof IfStatement)  {
          IfStatement ifStat = (IfStatement)curr;
          Exprent condition = ifStat.getHeadexprent().getCondition();

          if (condition instanceof FunctionExprent && ((FunctionExprent)condition).getFuncType() == FunctionType.NE) {
            condition = ((FunctionExprent)condition).getLstOperands().get(0);
          }

          if (condition instanceof InvocationExprent && ((InvocationExprent)condition).getLstParameters().size() == 1) {
            Exprent assign = ifStat.getIfstat().getExprents().get(0);
            int caseVal = ((ConstExprent)((AssignmentExprent)assign).getRight()).getIntValue();
            caseMap.put(caseVal, ((InvocationExprent)condition).getLstParameters().get(0));
          }

          curr = ifStat.getElsestat();
        }
      }

      if (nullable) {
        // the else branch of the containing `if` will have an assignment exprent to get the null case from
        Statement elseBranch = containingNullCheck.getElsestat();
        AssignmentExprent assign = (AssignmentExprent) elseBranch.getExprents().get(0);
        caseMap.put(((ConstExprent)assign.getRight()).getIntValue(), new ConstExprent(VarType.VARTYPE_NULL, null, null));
      }

      List<List<Exprent>> realCaseValues = following.getCaseValues().stream()
        .map(l -> l.stream()
          .map(e -> e instanceof ConstExprent ? ((ConstExprent)e).getIntValue() : null)
          .map(caseMap::get)
          .collect(Collectors.toList()))
        .collect(Collectors.toList());

      following.getCaseValues().clear();
      following.getCaseValues().addAll(realCaseValues);

      Exprent followingVal = ((SwitchHeadExprent)following.getHeadexprent()).getValue();
      following.getHeadexprent().replaceExprent(followingVal, ((InvocationExprent)value).getInstance());

      List<Exprent> firsts = switchStatement.getFirst().getExprents();
      if (firsts.size() > 0) {
        firsts.remove(firsts.size() - 1);
      }
      switchStatement.getFirst().getAllPredecessorEdges().forEach(switchStatement.getFirst()::removePredecessor);
      switchStatement.getFirst().getAllSuccessorEdges().forEach(switchStatement.getFirst()::removeSuccessor);
      switchStatement.getParent().replaceStatement(switchStatement, switchStatement.getFirst());

      if (nullable) {
        // remove the containing `if`
        Statement replaced = containingNullCheck.replaceWithEmpty();

        // Replace unneeded edges left over from the replaced block
        new HashSet<>(replaced.getLabelEdges())
          .forEach(e -> {
            following.removePredecessor(e);
            e.removeClosure();
          });
      }

      // Remove phantom references from old switch statement, but ignoring the first statement as that has been extracted out of the switch
      following.getAllPredecessorEdges().stream()
        .filter(e -> switchStatement.containsStatement(e.getSource()) && e.getSource() != switchStatement.getFirst())
        .forEach(e -> e.getSource().removeSuccessor(e));

      return true;
    }
    return false;
  }

  public static final int STATIC_FINAL_SYNTHETIC = CodeConstants.ACC_STATIC | CodeConstants.ACC_FINAL | CodeConstants.ACC_SYNTHETIC;
  /**
   * When Java introduced Enums they added the ability to use them in Switch statements.
   * This was done in a purely syntax sugar way using the old switch on int methods.
   * The compiler creates a synthetic class with a static int array field.
   * To support enums changing post compile, It initializes this field with a length of the current enum length.
   * And then for every referenced enum value it adds a mapping in the form of:
   *   try {
   *     field[Enum.VALUE.ordinal()] = 1;
   *   } catch (FieldNotFoundException e) {}
   *
   * If a class has multiple switches on multiple enums, the compiler adds the init and try list to the BEGINNING of the static initalizer.
   * But they add the field to the END of the fields list.
   * 
   * Note: SOME compilers name the field starting with $SwitchMap, so if we do not have full context this can be a guess.
   * But Obfuscated/renamed code could cause issues
   */
  private static boolean isEnumArray(Exprent exprent) {
    if (exprent instanceof ArrayExprent) {
      ArrayExprent arr = (ArrayExprent) exprent;
      Exprent tmp = arr.getArray();
      if (tmp instanceof FieldExprent) {
        FieldExprent field = (FieldExprent)tmp;
        Exprent index = arr.getIndex();
        ClassesProcessor.ClassNode classNode = DecompilerContext.getClassProcessor().getMapRootClasses().get(field.getClassname());
        
        if (classNode == null || !"[I".equals(field.getDescriptor().descriptorString)) {
          return field.getName().startsWith("$SwitchMap") || //This is non-standard but we don't have any more information so..
            (index instanceof InvocationExprent && ((InvocationExprent) index).getName().equals("ordinal"));
        }

        StructField stField;
        if (classNode.getWrapper() == null) { // I have no idea why this happens, according to debug tests it doesn't even return null
          stField = classNode.classStruct.getField(field.getName(), field.getDescriptor().descriptorString);
        } else {
          stField = classNode.getWrapper().getClassStruct().getField(field.getName(), field.getDescriptor().descriptorString);
        }

        if ((stField.getAccessFlags() & STATIC_FINAL_SYNTHETIC) != STATIC_FINAL_SYNTHETIC) {
          return false;
        }

        boolean isSyntheticClass;
        if (classNode.getWrapper() == null) {
          isSyntheticClass = (classNode.classStruct.getAccessFlags() & CodeConstants.ACC_SYNTHETIC) == CodeConstants.ACC_SYNTHETIC;
        } else {
          isSyntheticClass = (classNode.getWrapper().getClassStruct().getAccessFlags() & CodeConstants.ACC_SYNTHETIC) == CodeConstants.ACC_SYNTHETIC;
        }

        if (isSyntheticClass) {
          return true; //TODO: Find a way to check the structure of the initalizer?
          //Exprent init = classNode.getWrapper().getStaticFieldInitializers().getWithKey(InterpreterUtil.makeUniqueKey(field.getName(), field.getDescriptor().descriptorString));
          //Above is null because we haven't preocess the class yet?
        }
      } else if (tmp instanceof InvocationExprent) {
        InvocationExprent inv = (InvocationExprent) tmp;
        if (inv.getName().startsWith("$SWITCH_TABLE$")) { // More nonstandard behavior. Seems like eclipse compiler stuff: https://bugs.eclipse.org/bugs/show_bug.cgi?id=544521 TODO: needs tests!
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Gets the enum array exprent (or null if not found) corresponding to
   * the switch head. If the switch head itself is an enum array, returns the head.
   * If it's a variable only assigned to an enum array, returns that array.
   */
  private static ArrayExprent getEnumArrayExprent(Exprent switchHead, RootStatement root) {
    Exprent candidate = switchHead;

    if (switchHead instanceof FunctionExprent) {
      // Check for switches on a ternary expression like `a != null ? ...SwitchMap[a.ordinal()] : -1` (nullable switch)
      FunctionExprent func = (FunctionExprent) switchHead;
      if (func.getFuncType() == FunctionExprent.FunctionType.TERNARY && func.getLstOperands().size() == 3) {
        List<Exprent> ops = func.getLstOperands();
        if (ops.get(0) instanceof FunctionExprent) {
          FunctionExprent nn = (FunctionExprent) ops.get(0);
          if (nn.getFuncType() == FunctionExprent.FunctionType.NE
                && nn.getLstOperands().get(0) instanceof VarExprent
                && nn.getLstOperands().get(1).getExprType().equals(VarType.VARTYPE_NULL)) {
            // TODO: consider if verifying the variable used is necessary
            // probably not, since the array is checked to be generated (?) so user written code shouldn't encounter bad resugaring
            if (ops.get(2) instanceof ConstExprent) {
              ConstExprent minusOne = (ConstExprent) ops.get(2);
              if (minusOne.getConstType().equals(VarType.VARTYPE_INT) && minusOne.getIntValue() == -1) {
                candidate = ops.get(1);
              }
            }
          }
        }
      }
    }

    if (switchHead instanceof VarExprent) {
      // Check for switches with intermediary assignment of enum array index
      // This happens with Kotlin when expressions on enums.
      VarExprent var = (VarExprent) switchHead;

      if (!"I".equals(var.getVarType().toString())) {
        // Enum array index must be int
        return null;
      }

      List<AssignmentExprent> assignments = getAssignmentsOfWithinOneStatement(root, var);

      if (!assignments.isEmpty()) {
        if (assignments.size() == 1) {
          AssignmentExprent assignment = assignments.get(0);
          candidate = assignment.getRight();
        } else {
          // more than 1 assignment to variable => can't be what we're looking for
          return null;
        }
      }
    }

    return isEnumArray(candidate) ? (ArrayExprent) candidate : null;
  }

  /**
   * Recursively searches for assignments of the target that happen within one statement.
   * This is done as a list because the intended outcomes of the "1 found" (unique) and "2+ found" (non-unique) cases
   * are different. (But we don't need to have all the assignments within the root stat
   * because the non-unique case is a failure.)
   */
  private static List<AssignmentExprent> getAssignmentsOfWithinOneStatement(Statement start, Exprent target) {
    List<AssignmentExprent> exprents = new ArrayList<>();
    findExprents(start, AssignmentExprent.class, assignment -> assignment.getLeft().equals(target), true, (stat, expr) -> exprents.add(expr));
    return exprents;
  }

  /**
   * Recursively searches one statement for matching exprents.
   *
   * @param start       the statement to search
   * @param exprClass   the wanted exprent type
   * @param predicate   a predicate for filtering the exprents
   * @param onlyOneStat if true, will return eagerly after the first matching statement
   * @param consumer    the consumer that receives the exprents and their parent statements
   */
  // TODO: move somewhere better
  @SuppressWarnings("unchecked")
  public static <T extends Exprent> void findExprents(Statement start, Class<? extends T> exprClass, Predicate<T> predicate, boolean onlyOneStat, BiConsumer<Statement, T> consumer) {
    Queue<Statement> statQueue = new ArrayDeque<>();
    statQueue.offer(start);

    while (!statQueue.isEmpty()) {
      Statement stat = statQueue.remove();
      statQueue.addAll(stat.getStats());

      if (stat.getExprents() != null) {
        boolean foundAny = false;

        for (Exprent expr : stat.getExprents()) {
          if (exprClass.isInstance(expr) && predicate.test((T) expr)) {
            consumer.accept(stat, (T) expr);
            foundAny = true;
          }
        }

        if (onlyOneStat && foundAny) {
          break;
        }
      }
    }
  }

  /**
   * Switch on string gets compiled into two sequential switch statements.
   *   The first is a switch on the hashcode of the string with the case statement
   *   being the actual if equal to string literal check. Hashcode collisions result in
   *   an else if chain. The body of the if block sets the switch variable for the
   *   following switch.
   *
   *   The second switch block has the case statements of the original switch on string.
   *
   *   byte b1 = -1;
   *   switch (stringVar.hashcode()) {
   *     case -390932093:
   *        if (stringVar.equals("foo") {
   *          b1 = 0;
   *        }
   *   }
   *
   *   switch(b1) {
   *     case 0 :
   *        // code for case "foo"
   *   }
   */
  private static boolean isSwitchOnString(SwitchStatement first) {
    SwitchStatement second = null;
    List<StatEdge> edges = first.getSuccessorEdges(StatEdge.TYPE_REGULAR);
    if (edges.size() == 1 && edges.get(0).getDestination() instanceof SwitchStatement) {
      second = (SwitchStatement)edges.get(0).getDestination();
    }
    AssignmentExprent nullAssign = null;

    // if we're the only thing in an if statement,
    if (first.getParent() instanceof IfStatement) {
      if (!first.hasSuccessor(StatEdge.TYPE_REGULAR)) {
        IfStatement parent = (IfStatement) first.getParent();
        Exprent ifCond = parent.getHeadexprent().getCondition();
        // and it's a null check with `else` branch,
        if (parent.iftype == IfStatement.IFTYPE_IFELSE && ifCond instanceof FunctionExprent) {
          FunctionExprent func = (FunctionExprent)ifCond;
          if (func.getFuncType() == FunctionType.NE && func.getLstOperands().size() == 2) {
            Exprent right = func.getLstOperands().get(1);
            if (right instanceof ConstExprent && right.getExprType() == VarType.VARTYPE_NULL) {
              // and the `else` only assigns a variable,
              Statement elseStat = parent.getElsestat();
              if (elseStat instanceof BasicBlockStatement && elseStat.getExprents().size() == 1) {
                Exprent assign = elseStat.getExprents().get(0);
                if (assign instanceof AssignmentExprent) {
                  nullAssign = (AssignmentExprent)assign;
                  // then we're probably a nullable string-switch
                  edges = parent.getSuccessorEdges(StatEdge.TYPE_REGULAR);
                  if (edges.size() == 1 && edges.get(0).getDestination() instanceof SwitchStatement) {
                    second = (SwitchStatement)edges.get(0).getDestination();
                  }
                }
              }
            }
          }
        }
      }
    }

    if (second != null) {
      Exprent firstValue = ((SwitchHeadExprent)first.getHeadexprent()).getValue();
      Exprent secondValue = ((SwitchHeadExprent)second.getHeadexprent()).getValue();

      if (firstValue instanceof InvocationExprent && secondValue instanceof VarExprent && first.getCaseStatements().get(0) instanceof IfStatement) {
        InvocationExprent invExpr = (InvocationExprent)firstValue;
        VarExprent varExpr = (VarExprent) secondValue;
        if (nullAssign != null && !nullAssign.getLeft().equals(varExpr)) {
          return false; // wrong assignment across `if`
        }

        if (invExpr.getName().equals("hashCode") && invExpr.getClassname().equals("java/lang/String")) {
          boolean matches = true;

          for (int i = 0; matches && i < first.getCaseStatements().size(); ++i) {
            if (!first.getCaseEdges().get(i).contains(first.getDefaultEdge())) {
              Statement curr = first.getCaseStatements().get(i);
              while (matches && curr != null) {
                if (curr instanceof IfStatement) {
                  IfStatement ifStat = (IfStatement)curr;
                  Exprent condition = ifStat.getHeadexprent().getCondition();

                  if (condition instanceof FunctionExprent && ((FunctionExprent)condition).getFuncType() == FunctionType.NE) {
                    condition = ((FunctionExprent)condition).getLstOperands().get(0);
                  }

                  if (condition instanceof InvocationExprent) {
                    InvocationExprent condInvocation = (InvocationExprent)condition;

                    if (condInvocation.getName().equals("equals") && condInvocation.getInstance().equals(invExpr.getInstance())) {
                      List<Exprent> block = ifStat.getIfstat().getExprents();

                      if (block != null && block.size() == 1 && block.get(0) instanceof AssignmentExprent) {
                        AssignmentExprent assign = (AssignmentExprent)block.get(0);

                        if (assign.getRight() instanceof ConstExprent && varExpr.equals(assign.getLeft())) {

                          curr = ifStat.getElsestat();
                          continue;
                        }
                      }
                    }
                  }
                }

                matches = false;
              }
            }
          }

          return matches;
        }
      }
    }

    return false;
  }
}
