fun test(list: MutableList<String>) {
    list.removeAll {
        it.isEmpty()
    }
}
