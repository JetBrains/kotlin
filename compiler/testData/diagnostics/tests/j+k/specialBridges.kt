// RUN_PIPELINE_TILL: BACKEND
// JAVAC_EXPECTED_FILE
interface I2 {
    val size: Int
}

class B2 : java.util.ArrayList<String>(), I2

/* GENERATED_FIR_TAGS: classDeclaration, interfaceDeclaration, propertyDeclaration */
