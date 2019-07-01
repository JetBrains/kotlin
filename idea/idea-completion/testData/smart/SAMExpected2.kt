import java.util.Comparator

var v: Comparator<String> = <caret>

// EXIST: { itemText: "object : Comparator<String>{...}" }
// EXIST: { lookupString: "Comparator", itemText: "Comparator", tailText: "(function: (T!, T!) -> Int) (java.util)", typeText: "Comparator<T>" }
// EXIST: { lookupString: "Comparator", itemText: "Comparator", tailText: " { T, T -> ... } (function: (T!, T!) -> Int) (java.util)", typeText: "Comparator<T>" }
