// "class org.jetbrains.kotlin.idea.quickfix.AddOpenModifierToClassDeclarationFix" "false"
// ERROR: This type is final, so it cannot be inherited from
// ACTION: Create Test
import testPackage.*

class foo : <caret>JavaClass() {}
