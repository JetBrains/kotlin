package pack

class DeclarationDescriptor

fun f(dd<caret>)

// EXIST: { lookupString: "declarationDescriptor", itemText: "declarationDescriptor: DeclarationDescriptor", tailText: " (pack)" }
