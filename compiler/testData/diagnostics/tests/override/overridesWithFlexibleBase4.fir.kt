// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-73760
// LANGUAGE: +AllowDnnTypeOverridingFlexibleType

// FILE: CustomObserver.java
import org.jetbrains.annotations.NotNull;

public interface CustomObserver<T> {
    void onNext(@NotNull T value);
}

// FILE: CustomSubscriber.java
public interface CustomSubscriber<T> {
    void onNext(T value);
}

// FILE: LoggingObserver.kt
class LoggingObserver<T> : CustomObserver<T>, CustomSubscriber<T> {
    override fun onNext(value: T & Any) {}
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class LoggingObserverLegacy<!><T> : CustomObserver<T>, CustomSubscriber<T> {
    override fun onNext(value: T) {}
}
