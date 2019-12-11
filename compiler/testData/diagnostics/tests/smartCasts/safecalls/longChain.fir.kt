fun calc(x: List<String>?) {
    // x should be non-null in arguments list, despite of a chain
    x?.subList(0, x.size)?.
       subList(0, x.size)?.
       get(x.size)
}
