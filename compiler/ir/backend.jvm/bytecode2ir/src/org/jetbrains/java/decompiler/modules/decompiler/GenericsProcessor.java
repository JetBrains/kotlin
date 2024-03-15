package org.jetbrains.java.decompiler.modules.decompiler;

import org.jetbrains.java.decompiler.modules.decompiler.exps.Exprent;
import org.jetbrains.java.decompiler.modules.decompiler.exps.InvocationExprent;
import org.jetbrains.java.decompiler.modules.decompiler.exps.NewExprent;
import org.jetbrains.java.decompiler.modules.decompiler.stats.Statement;
import org.jetbrains.java.decompiler.struct.StructMethod;
import org.jetbrains.java.decompiler.struct.gen.VarType;
import org.jetbrains.java.decompiler.struct.gen.generics.GenericMethodDescriptor;

import java.util.ArrayList;
import java.util.List;

public final class GenericsProcessor {
  // Comparator.comparing(...).thenComparing(...) -> Comparator.<...>comparing(...).thenComparing(...)
  public static boolean qualifyChains(Statement stat) {
    boolean res = false;

    if (stat.getExprents() != null) {
      for (Exprent exprent : stat.getExprents()) {
        res |= qualifyChain(exprent);
      }
    }

    for (Statement st : stat.getStats()) {
      res |= qualifyChains(st);
    }

    return res;
  }

  private static boolean qualifyChain(Exprent exp) {
    boolean res = false;

    for (Exprent expr : exp.getAllExprents()) {
      res |= qualifyChain(expr);
    }

    if (exp instanceof InvocationExprent) {
      res |= qualifyChain((InvocationExprent) exp);
    }

    return res;
  }

  private static boolean qualifyChain(InvocationExprent invoc) {
    List<InvocationExprent> chain = new ArrayList<>();

    chain.add(invoc);
    InvocationExprent temp = invoc;
    while (temp.getInstance() != null && temp.getInstance() instanceof InvocationExprent) {
      temp = (InvocationExprent) temp.getInstance();
      chain.add(temp);
    }

    if (chain.size() <= 1) {
      return false;
    }

    InvocationExprent last = chain.get(chain.size() - 1);
    boolean foundLambda = false;
    for (Exprent parameter : last.getLstParameters()) {
      if (parameter instanceof NewExprent && ((NewExprent) parameter).isLambda()) {
        if (((NewExprent) parameter).isMethodReference()) {
          if (!((NewExprent) parameter).doesClassHaveMethodsNamedSame()) {
            continue;
          }
        }

        foundLambda = true;
        break;
      }
    }
    if (!foundLambda) {
      return false;
    }

    last.getInferredExprType(null);
    if (last.getDesc() == null) {
      return false;
    }

    if (last.getDesc().getSignature() == null) {
      return false;
    }

    List<String> baseTypes = last.getDesc().getSignature().typeParameters;
    VarType retType = last.getDescriptor().ret;

    for (InvocationExprent expr : chain) {
      if (!expr.getDescriptor().ret.isSuperset(retType)) {
        return false;
      }
    }

    for (InvocationExprent expr : chain) {
      // Set descriptor
      // TODO: is this doing too much? should we just set manually?
      expr.getInferredExprType(null);

      StructMethod mt = expr.getDesc();
      if (mt == null) {
        return false;
      }

      GenericMethodDescriptor descriptor = mt.getSignature();

      if (descriptor == null) {
        return false;
      }
      
      List<String> curParams = descriptor.typeParameters;
      for (String curParam : curParams) {
        if (baseTypes.contains(curParam)) {
          last.forceGenericQualfication = true;
          return true;
        }
      }
    }

    return false;
  }
}
