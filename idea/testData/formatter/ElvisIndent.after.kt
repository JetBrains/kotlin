fun foo() {
    val topLevelDeclaration =
        DescriptorUtils.getParentOfType(referencedDescriptor, PropertyDescriptor::class.java, false) as DeclarationDescriptor?
                ?: DescriptorUtils.getParentOfType(
                    referencedDescriptor,
                    TypeAliasConstructorDescriptor::class.java,
                    false,
                )?.typeAliasDescriptor
                ?: DescriptorUtils.getParentOfType(referencedDescriptor, FunctionDescriptor::class.java, false)
                ?: return emptyList()
}

// SET_FALSE: CONTINUATION_INDENT_IN_ARGUMENT_LISTS
// SET_FALSE: CONTINUATION_INDENT_FOR_EXPRESSION_BODIES
