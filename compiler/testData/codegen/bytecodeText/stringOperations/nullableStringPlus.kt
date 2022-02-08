fun foo(x: String?, y: Any?) = x + y

// JVM_TEMPLATES
// 1 stringPlus

// JVM_IR_TEMPLATES
// 1 NEW java/lang/StringBuilder
// 1 INVOKEVIRTUAL java/lang/StringBuilder\.append \(Ljava/lang/String;\)Ljava/lang/StringBuilder;
// 1 INVOKEVIRTUAL java/lang/StringBuilder\.append \(Ljava/lang/Object;\)Ljava/lang/StringBuilder;
// 1 INVOKEVIRTUAL java/lang/StringBuilder\.toString \(\)Ljava/lang/String;
