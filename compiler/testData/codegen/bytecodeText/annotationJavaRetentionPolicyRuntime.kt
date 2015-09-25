import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

@Ann class MyClass

@Retention(RetentionPolicy.RUNTIME)
annotation class Ann

// 1 @LAnn;()