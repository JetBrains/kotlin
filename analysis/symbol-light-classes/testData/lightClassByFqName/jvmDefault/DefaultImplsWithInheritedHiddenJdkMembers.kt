// B

// Before JDK 21, FIR adds hidden `List.getFirst` and `List.getLast`, which don't exist in the bytecode
interface B : List<String>
