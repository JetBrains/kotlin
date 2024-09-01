// FULL_JDK
// WITH_STDLIB
// SCRIPT_DEFAULT_IMPORTS: kotlin.streams.asSequence

import java.util.stream.Stream

Stream.of("foo").asSequence().apply {
    <expr>contains("bar")</expr>
}