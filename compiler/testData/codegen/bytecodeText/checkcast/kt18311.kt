fun unnecessaryCasts(strings: List<String>): List<String> {
    return strings.filterTo(ArrayList<String>(), { it.length > 5 })
}

// 2 CHECKCAST
