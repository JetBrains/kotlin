namespace unwelcome.guest

import kotlin.io.*

val GUEST_USER_ID = -1
val USER_ID =
    try {
        getUserIdFromEnvironment()
    }
    catch (e : UnsupportedOperationException) {
//    catch (e : IdUnavailableException) {  BUG
        GUEST_USER_ID
    }

fun getUserIdFromEnvironment() = throw UnsupportedOperationException()
//fun getUserIdFromEnvironment() = throw IdUnavailableException()

//class IdUnavailableException() : Ex() {}

fun main(args : Array<String>) {
    println("User ID: " + USER_ID)
}