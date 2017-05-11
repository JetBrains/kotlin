// ERROR: The feature "array literals in annotations" is only available since language version 1.2

annotation class Some(val nums: IntArray)

@Some(nums = <caret>intArrayOf(1, 2))
class My
