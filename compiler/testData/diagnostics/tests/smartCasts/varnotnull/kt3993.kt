class User(val login : Boolean) {}

fun currentAccess(user: User?): Int {
    return when {
        user == null -> 0
        // We should get smartcast here
        <!DEBUG_INFO_SMARTCAST!>user<!>.login -> 1 
        else -> -1
    }
}