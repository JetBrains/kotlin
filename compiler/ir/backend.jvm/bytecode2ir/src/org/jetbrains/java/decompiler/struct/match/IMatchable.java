// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.struct.match;

public interface IMatchable {
  enum MatchProperties {
    STATEMENT_TYPE,
    STATEMENT_RET,
    STATEMENT_STATSIZE,
    STATEMENT_EXPRSIZE,
    STATEMENT_POSITION,
    STATEMENT_IFTYPE,

    EXPRENT_TYPE,
    EXPRENT_RET,
    EXPRENT_POSITION,
    EXPRENT_FUNCTYPE,
    EXPRENT_EXITTYPE,
    EXPRENT_CONSTTYPE,
    EXPRENT_CONSTVALUE,
    EXPRENT_INVOCATION_CLASS,
    EXPRENT_INVOCATION_SIGNATURE,
    EXPRENT_INVOCATION_PARAMETER,
    EXPRENT_VAR_INDEX,
    EXPRENT_FIELD_NAME,
  }

  IMatchable findObject(MatchNode matchNode, int index);

  boolean match(MatchNode matchNode, MatchEngine engine);
}