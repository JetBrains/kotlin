package pack

class DeclarationDescriptor

fun f(dd<caret>)

// EXIST: { lookupString: "declarationDescriptor : DeclarationDescriptor", itemText: "declarationDescriptor: DeclarationDescriptor", tailText: " (pack)" }
