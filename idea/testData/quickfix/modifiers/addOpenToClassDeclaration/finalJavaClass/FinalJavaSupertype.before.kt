// "class org.jetbrains.jet.plugin.quickfix.AddOpenModifierToClassDeclarationFix" "false"
// ERROR: This type is final, so it cannot be inherited from
import testPackage.*

class foo : <caret>JavaClass() {}
