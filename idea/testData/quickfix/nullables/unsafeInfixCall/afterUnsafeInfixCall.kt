// "Replace with safe (?.) call" "true"
fun test(a : Int?) : Int? {
    return a?.compareTo(6);
}