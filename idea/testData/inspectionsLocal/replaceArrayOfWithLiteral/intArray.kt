// LANGUAGE_VERSION: 1.2

annotation class Some(val nums: IntArray)

@Some(nums = <caret>intArrayOf(1, 2))
class My
