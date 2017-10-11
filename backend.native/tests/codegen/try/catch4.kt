package codegen.`try`.catch4

import kotlin.test.*

@Test fun runTest() {
    try {
        println("Before")
        throw Error("Error happens")
        println("After")
    } catch (e: Exception) {
        println("Caught Exception")
    } catch (e: Error) {
        println("Caught Error")
    } catch (e: Throwable) {
        println("Caught Throwable")
    }

    println("Done")
}