/**
 * "Bottles.kt" that doesn't compile, see line 6.
 */
package bottles

fun main(args : Array<String>
  if (args.isEmpty) {
    printBottles(99)
  }
  else {
    try {
      printBottles(Integer.parseInt(args[0]))
    }
    catch (e : NumberFormatException) {
      System.err?.println("You have passed '${args[0]}' as a number of bottles, " +
                          "but it is not a valid integral number")
    }
  }
}

fun printBottles(bottleCount : Int) {
  if (bottleCount <= 0) {
    println("No bottles - no song")
    return
  }

  println("The \"${bottlesOfBeer(bottleCount)}\" song\n")

  var bottles = bottleCount
  while (bottles > 0) {
    val bottlesOfBeer = bottlesOfBeer(bottles)
    print("$bottlesOfBeer on the wall, $bottlesOfBeer.\nTake one down, pass it around, ")
    bottles--
    println("${bottlesOfBeer(bottles)} on the wall.\n")
  }
  println("No more bottles of beer on the wall, no more bottles of beer.\n" +
          "Go to the store and buy some more, ${bottlesOfBeer(bottleCount)} on the wall.")
}

fun bottlesOfBeer(count : Int) : String =
  when (count) {
           0 -> "no more bottles"
           1 -> "1 bottle"
           else -> "$count bottles"
         } + " of beer"

/*
 * An excerpt from the Standard Library
 */

// From the std.io package
// These are simple functions that wrap standard Java API calls
fun print(message : String) { System.out?.print(message) }
fun println(message : String) { System.out?.println(message) }

// From the std package
// This is an extension property, i.e. a property that is defined for the
// type Array<T>, but does not sit inside the class Array
val <T> Array<T>.isEmpty : Boolean get() = size == 0
