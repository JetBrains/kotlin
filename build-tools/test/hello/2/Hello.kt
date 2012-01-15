fun main(args : Array<String>) {
  if (args.size == 0) {
    System.out?.println("Please provide a name as a command-line argument")
    return
  }
  System.out?.println("Hello, ${args[0]}!")
}
