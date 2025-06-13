// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
import java.io.File
import kotlin.system.exitProcess

object Main {
    private val KOTLIN_HOME: File

    init {
        val home = System.getProperty("kotlin.home")
        if (home == null) {
            exitProcess(1)
        }
        KOTLIN_HOME = File(home)
    }
}

/* GENERATED_FIR_TAGS: assignment, equalityExpression, flexibleType, ifExpression, init, integerLiteral, javaFunction,
localProperty, objectDeclaration, propertyDeclaration, smartcast, stringLiteral */
