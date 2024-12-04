// This should not compile: only JVM 1.8+ contains getOrDefault
fun foo(map: Map<String, Int>) = map.getOrDefault("", 0)
