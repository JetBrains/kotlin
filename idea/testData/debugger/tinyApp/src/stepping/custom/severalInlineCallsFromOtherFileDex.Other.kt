package severalInlineCallsFromOtherFileDex

inline fun inlineFun() {
    var i = 1
    // Breakpoint 1
    i++
    i++
}