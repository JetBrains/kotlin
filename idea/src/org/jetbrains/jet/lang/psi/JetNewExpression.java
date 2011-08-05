package org.jetbrains.jet.lang.psi;

/**
 * @author max
 */
//public class JetNewExpression extends JetExpression implements JetCall {
//    public JetNewExpression(@NotNull ASTNode node) {
//        super(node);
//    }
//
//    @Override
//    public void accept(JetVisitor visitor) {
//        visitor.visitNewExpression(this);
//    }
//
//    @Nullable @IfNotParsed
//    public JetTypeReference getTypeReference() {
//        return (JetTypeReference) findChildByType(JetNodeTypes.TYPE_REFERENCE);
//    }
//
//    @Override
//    @Nullable
//    public JetValueArgumentList getValueArgumentList() {
//        return (JetValueArgumentList) findChildByType(JetNodeTypes.VALUE_ARGUMENT_LIST);
//    }
//
//    @Override
//    @NotNull
//    public List<JetValueArgument> getValueArguments() {
//        JetValueArgumentList list = getValueArgumentList();
//        return list != null ? list.getArguments() : Collections.<JetValueArgument>emptyList();
//    }
//
//    @Override
//    @NotNull
//    public List<JetExpression> getFunctionLiteralArguments() {
//        return findChildrenByType(JetNodeTypes.FUNCTION_LITERAL_EXPRESSION);
//    }
//
//    @NotNull
//    @Override
//    public List<JetTypeProjection> getTypeArguments() {
//        return Collections.emptyList();
//    }
//
//    @NotNull
//    @Override
//    public JetElement asElement() {
//        return this;
//    }
//
//}
