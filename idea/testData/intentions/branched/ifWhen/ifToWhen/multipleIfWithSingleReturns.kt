fun bar(arg: Int): Int {
    <caret>if (arg == 1) return 2
    if (arg == 2) return 5
    if (arg == 3) return 9
    return 13
}