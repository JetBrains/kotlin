// "Replace with safe (?.) call" "true"
// WITH_RUNTIME
class T(s: String?) {
    var i: Int = s<caret>.length
}
/* FIR_COMPARISON */