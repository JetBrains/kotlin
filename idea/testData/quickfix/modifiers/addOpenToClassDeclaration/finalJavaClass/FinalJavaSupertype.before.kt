// "Make JavaClass open" "false"
// ERROR: This type is final, so it cannot be inherited from
import testPackage.*

class foo : <caret>JavaClass() {}
