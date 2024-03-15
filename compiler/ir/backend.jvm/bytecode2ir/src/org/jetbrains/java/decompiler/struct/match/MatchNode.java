// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.struct.match;

import org.jetbrains.java.decompiler.struct.match.IMatchable.MatchProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MatchNode {
  public static class RuleValue {
    public final int parameter;
    public final Object value;

    public RuleValue(int parameter, Object value) {
      this.parameter = parameter;
      this.value = value;
    }

    public boolean isVariable() {
      String strValue = value.toString();
      return (strValue.charAt(0) == '$' && strValue.charAt(strValue.length() - 1) == '$');
    }

    public String toString() {
      return value.toString();
    }
  }

  public static final int MATCHNODE_STATEMENT = 0;
  public static final int MATCHNODE_EXPRENT = 1;

  private final int type;
  private final Map<MatchProperties, RuleValue> rules = new HashMap<>();
  private final List<MatchNode> children = new ArrayList<>();

  public MatchNode(int type) {
    this.type = type;
  }

  public void addChild(MatchNode child) {
    children.add(child);
  }

  public void addRule(MatchProperties property, RuleValue value) {
    rules.put(property, value);
  }

  public int getType() {
    return type;
  }

  public List<MatchNode> getChildren() {
    return children;
  }

  public Map<MatchProperties, RuleValue> getRules() {
    return rules;
  }

  public Object getRuleValue(MatchProperties property) {
    RuleValue rule = rules.get(property);
    return rule == null ? null : rule.value;
  }
}