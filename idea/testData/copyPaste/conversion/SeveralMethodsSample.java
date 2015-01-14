class A {
    <selection>private static String formatElement(PsiElement element) {
        element = JetPsiUtil.ascendIfPropertyAccessor(element);
        if (element instanceof JetNamedFunction || element instanceof JetProperty) {
            BindingContext bindingContext =
                    AnalyzerFacadeWithCache.analyzeFileWithCache(element.getContainingJetFile()).getBindingContext();

            DeclarationDescriptor declarationDescriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, element);
            if (declarationDescriptor instanceof CallableMemberDescriptor) {
                DeclarationDescriptor containingDescriptor = declarationDescriptor.getContainingDeclaration();
                if (containingDescriptor instanceof ClassDescriptor) {
                    return JetBundle.message(
                            "x.in.y",
                            DescriptorRenderer.COMPACT.render(declarationDescriptor),
                            IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.render(containingDescriptor)
                    );
                }
            }
        }

        assert element instanceof PsiMethod
                : "Method accepts only kotlin functions/properties and java methods, but '" + element.getText() + "' was found";
        return JetRefactoringUtil.formatPsiMethod((PsiMethod) element, true, false);
    }

    @Override
    protected String getDimensionServiceKey() {
        return "#org.jetbrains.kotlin.idea.refactoring.safeDelete.KotlinOverridingDialog";
    }

    public ArrayList<UsageInfo> getSelected() {
        ArrayList<UsageInfo> result = new ArrayList<UsageInfo>();
        for (int i = 0; i < myChecked.length; i++) {
            if (myChecked[i]) {
                result.add(myOverridingMethods.get(i));
            }
        }
        return result;
    }</selection>
}
