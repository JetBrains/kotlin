class Person(
    copy var name: String,
    copy var title: String?,
) {
    copy var visibleName: String

    copy fun capitalize() { }

    fun test() {
        val f = { copy x -> 0 }
    }
}

copy fun Person.doSomething() { }
