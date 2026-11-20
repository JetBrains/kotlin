fun <T> function(a: AA): AA where AA : @Anno("function constraint") List<@Anno("nested function constraint") T>, AA : Comparable<AA> {}

fun <T> annotatedConstraint() where @Anno("dangling constraint") AA : Any, @Anno("declared constraint") T : Any {}

val <T> T.property: AA where AA : @Anno("property constraint") Number get() = null!!

class TopLevelClass<T> where AA : @Anno("class constraint") Any {
    fun <R> memberFunction(b: BB) where BB : @Anno("member function constraint") R {}
}

class OuterClass<T> where AA : Any {
    inner class InnerClass(val a: AA) {
        constructor() : this(null!!)
    }
}

enum class EnumClass<> where AA : Any {
    ENTRY {
        fun member(a: AA) {}
    }
}
