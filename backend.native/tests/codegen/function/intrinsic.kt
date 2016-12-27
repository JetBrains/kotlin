
// This code fails to link when bultins are taken from the 
// frontend generated module, instead of our library.
// Because the generated module doesn't see our name changing annotations,
// the function names are all wrong.

fun intrinsic(b: Int): Int {
  var sum = 1
  sum = sum + b
  return sum
}

fun main(args: Array<String>) {
  if (intrinsic(3) != 4) throw Error()
}