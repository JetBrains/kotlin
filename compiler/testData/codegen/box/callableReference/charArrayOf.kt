// IGNORE_BACKEND: JS

fun box(): String {
    val ref: (CharArray) -> CharArray = ::charArrayOf
    val arr = ref(charArrayOf('O', 'K'))
    return "${arr[0]}${arr[1]}"
}
