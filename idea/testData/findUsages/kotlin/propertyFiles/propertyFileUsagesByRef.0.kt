// PSI_ELEMENT: com.intellij.lang.properties.psi.PropertiesFile
// FIND_BY_REF
import org.jetbrains.annotations.PropertyKey

public fun message(@PropertyKey(resourceBundle = "<caret>idea.testData.findUsages.kotlin.propertyFiles.propertyFileUsagesByRef.2") key: String) = key

fun test() {
    message("foo.bar")
}