// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// JAVAC_EXPECTED_FILE
interface I2 {
    val size: Int
}

class B2 : java.util.ArrayList<String>(), I2

