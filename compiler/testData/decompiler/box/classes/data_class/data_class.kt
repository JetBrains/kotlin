data class MyUser(val name: String, val age: Int)

data class MyUserDefaultName(val name: String = "John", val age: Int = 42)
fun box(): String {
    val myUser = MyUser("John", 42)
    val (name, age) = MyUser("John", 42)
    if (name != myUser.name || age != myUser.age) {
        return "FAIL"
    }

    val (name1, age1) = myUser
    if (name != name1 || age != age1) {
        return "FAIL"
    }

    val myUserDefaultName = MyUserDefaultName()
    if (myUserDefaultName.name != myUser.name || myUserDefaultName.age != myUser.age) {
        return "FAIL"
    }
    return "OK"

}