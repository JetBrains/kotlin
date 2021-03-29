// "Replace with safe (?.) call" "true"
// WITH_RUNTIME
class T(s: String?) {
    var i = s<caret>.length
}
