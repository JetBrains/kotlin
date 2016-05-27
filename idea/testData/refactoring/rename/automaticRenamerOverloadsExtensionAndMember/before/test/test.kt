package test

class MemberExtension {
    fun funMe(p: Int) {}
}

fun MemberExtension./*rename*/funMe(p: String) {}