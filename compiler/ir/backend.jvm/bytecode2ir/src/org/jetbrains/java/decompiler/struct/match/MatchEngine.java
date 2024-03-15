// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.struct.match;

import org.jetbrains.java.decompiler.modules.decompiler.exps.ExitExprent;
import org.jetbrains.java.decompiler.modules.decompiler.exps.Exprent;
import org.jetbrains.java.decompiler.modules.decompiler.exps.FunctionExprent.FunctionType;
import org.jetbrains.java.decompiler.modules.decompiler.stats.IfStatement;
import org.jetbrains.java.decompiler.modules.decompiler.stats.Statement;
import org.jetbrains.java.decompiler.struct.gen.VarType;
import org.jetbrains.java.decompiler.struct.match.IMatchable.MatchProperties;
import org.jetbrains.java.decompiler.struct.match.MatchNode.RuleValue;

import java.util.*;

public class MatchEngine {
  private static final Map<String, MatchProperties> stat_properties = new HashMap<>();
  private static final Map<String, MatchProperties> expr_properties = new HashMap<>();
  private static final Map<String, Statement.StatementType> stat_type = new HashMap<>();
  private static final Map<String, Exprent.Type> expr_type = new HashMap<>();
  private static final Map<String, FunctionType> expr_func_type = new HashMap<>();
  private static final Map<String, ExitExprent.Type> expr_exit_type = new HashMap<>();
  private static final Map<String, Integer> stat_if_type = new HashMap<>();
  private static final Map<String, VarType> expr_const_type = new HashMap<>();

  static {
    stat_properties.put("type", MatchProperties.STATEMENT_TYPE);
    stat_properties.put("ret", MatchProperties.STATEMENT_RET);
    stat_properties.put("position", MatchProperties.STATEMENT_POSITION);
    stat_properties.put("statsize", MatchProperties.STATEMENT_STATSIZE);
    stat_properties.put("exprsize", MatchProperties.STATEMENT_EXPRSIZE);
    stat_properties.put("iftype", MatchProperties.STATEMENT_IFTYPE);

    expr_properties.put("type", MatchProperties.EXPRENT_TYPE);
    expr_properties.put("ret", MatchProperties.EXPRENT_RET);
    expr_properties.put("position", MatchProperties.EXPRENT_POSITION);
    expr_properties.put("functype", MatchProperties.EXPRENT_FUNCTYPE);
    expr_properties.put("exittype", MatchProperties.EXPRENT_EXITTYPE);
    expr_properties.put("consttype", MatchProperties.EXPRENT_CONSTTYPE);
    expr_properties.put("constvalue", MatchProperties.EXPRENT_CONSTVALUE);
    expr_properties.put("invclass", MatchProperties.EXPRENT_INVOCATION_CLASS);
    expr_properties.put("signature", MatchProperties.EXPRENT_INVOCATION_SIGNATURE);
    expr_properties.put("parameter", MatchProperties.EXPRENT_INVOCATION_PARAMETER);
    expr_properties.put("index", MatchProperties.EXPRENT_VAR_INDEX);
    expr_properties.put("name", MatchProperties.EXPRENT_FIELD_NAME);

    stat_type.put("if", Statement.StatementType.IF);
    stat_type.put("do", Statement.StatementType.DO);
    stat_type.put("switch", Statement.StatementType.SWITCH);
    stat_type.put("trycatch", Statement.StatementType.TRY_CATCH);
    stat_type.put("basicblock", Statement.StatementType.BASIC_BLOCK);
    stat_type.put("sequence", Statement.StatementType.SEQUENCE);

    expr_type.put("annotation", Exprent.Type.ANNOTATION);
    expr_type.put("array", Exprent.Type.ARRAY);
    expr_type.put("assert", Exprent.Type.ASSERT);
    expr_type.put("assignment", Exprent.Type.ASSIGNMENT);
    expr_type.put("constant", Exprent.Type.CONST);
    expr_type.put("exit", Exprent.Type.EXIT);
    expr_type.put("field", Exprent.Type.FIELD);
    expr_type.put("function", Exprent.Type.FUNCTION);
    expr_type.put("if", Exprent.Type.IF);
    expr_type.put("invocation", Exprent.Type.INVOCATION);
    expr_type.put("monitor", Exprent.Type.MONITOR);
    expr_type.put("new", Exprent.Type.NEW);
    expr_type.put("switch", Exprent.Type.SWITCH);
    expr_type.put("switchhead", Exprent.Type.SWITCH_HEAD);
    expr_type.put("var", Exprent.Type.VAR);
    expr_type.put("yield", Exprent.Type.YIELD);

    expr_func_type.put("eq", FunctionType.EQ);

    expr_exit_type.put("return", ExitExprent.Type.RETURN);
    expr_exit_type.put("throw", ExitExprent.Type.THROW);

    stat_if_type.put("if", IfStatement.IFTYPE_IF);
    stat_if_type.put("ifelse", IfStatement.IFTYPE_IFELSE);

    expr_const_type.put("null", VarType.VARTYPE_NULL);
    expr_const_type.put("string", VarType.VARTYPE_STRING);
  }

  private final MatchNode rootNode;
  private final Map<String, Object> variables = new HashMap<>();

  public MatchEngine(String description) {
    // each line is a separate statement/exprent
    String[] lines = description.split("\n");

    int depth = 0;
    LinkedList<MatchNode> stack = new LinkedList<>();

    for (String line : lines) {
      List<String> properties = new ArrayList<>(Arrays.asList(line.split("\\s+"))); // split on any number of whitespaces
      if (properties.get(0).isEmpty()) {
        properties.remove(0);
      }

      int node_type = "statement".equals(properties.get(0)) ? MatchNode.MATCHNODE_STATEMENT : MatchNode.MATCHNODE_EXPRENT;

      // create new node
      MatchNode matchNode = new MatchNode(node_type);
      for (int i = 1; i < properties.size(); ++i) {
        String[] values = properties.get(i).split(":");

        MatchProperties property = (node_type == MatchNode.MATCHNODE_STATEMENT ? stat_properties : expr_properties).get(values[0]);
        if (property == null) { // unknown property defined
          throw new RuntimeException("Unknown matching property");
        }
        else {
          Object value;
          int parameter = 0;

          String strValue = values[1];
          if (values.length == 3) {
            parameter = Integer.parseInt(values[1]);
            strValue = values[2];
          }

          switch (property) {
            case STATEMENT_TYPE:
              value = stat_type.get(strValue);
              break;
            case STATEMENT_STATSIZE:
            case STATEMENT_EXPRSIZE:
              value = Integer.valueOf(strValue);
              break;
            case STATEMENT_POSITION:
            case EXPRENT_POSITION:
            case EXPRENT_INVOCATION_CLASS:
            case EXPRENT_INVOCATION_SIGNATURE:
            case EXPRENT_INVOCATION_PARAMETER:
            case EXPRENT_VAR_INDEX:
            case EXPRENT_FIELD_NAME:
            case EXPRENT_CONSTVALUE:
            case STATEMENT_RET:
            case EXPRENT_RET:
              value = strValue;
              break;
            case STATEMENT_IFTYPE:
              value = stat_if_type.get(strValue);
              break;
            case EXPRENT_FUNCTYPE:
              value = expr_func_type.get(strValue);
              break;
            case EXPRENT_EXITTYPE:
              value = expr_exit_type.get(strValue);
              break;
            case EXPRENT_CONSTTYPE:
              value = expr_const_type.get(strValue);
              break;
            case EXPRENT_TYPE:
              value = expr_type.get(strValue);
              break;
            default:
              throw new RuntimeException("Unhandled matching property");
          }

          matchNode.addRule(property, new RuleValue(parameter, value));
        }
      }

      if (stack.isEmpty()) { // first line, root node
        stack.push(matchNode);
      }
      else {
        // return to the correct parent on the stack
        int new_depth = line.lastIndexOf(' ', depth) + 1;
        for (int i = new_depth; i <= depth; ++i) {
          stack.pop();
        }

        // insert new node
        stack.getFirst().addChild(matchNode);
        stack.push(matchNode);

        depth = new_depth;
      }
    }

    this.rootNode = stack.getLast();
  }

  public boolean match(IMatchable object) {
    variables.clear();
    return match(this.rootNode, object);
  }

  private boolean match(MatchNode matchNode, IMatchable object) {
    if (!object.match(matchNode, this)) {
      return false;
    }

    int expr_index = 0;
    int stat_index = 0;
    for (MatchNode childNode : matchNode.getChildren()) {
      boolean isStatement = childNode.getType() == MatchNode.MATCHNODE_STATEMENT;

      IMatchable childObject = object.findObject(childNode, isStatement ? stat_index : expr_index);
      if (childObject == null || !match(childNode, childObject)) {
        return false;
      }

      if (isStatement) {
        stat_index++;
      }
      else {
        expr_index++;
      }
    }

    return true;
  }

  public boolean checkAndSetVariableValue(String name, Object value) {
    Object old_value = variables.get(name);
    if (old_value != null) {
      return old_value.equals(value);
    }
    else {
      variables.put(name, value);
      return true;
    }
  }

  public Object getVariableValue(String name) {
    return variables.get(name);
  }
}