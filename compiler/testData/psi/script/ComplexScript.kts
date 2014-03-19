fun die(message: String) {
    System.err.println(message)
    System.exit(1)
}

if (args.length == 0) {
    die("Need arguments")
}

for (arg in args) {
    println(arg)
}
