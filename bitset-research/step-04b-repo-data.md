# Шаг 4b. Извлечение данных по репозиториям

**Резюме.** Извлечены данные об использовании `java.util.BitSet` из 23 open-source репозиториев. Обработано 641 файлов: 422 use, 23 impl, 178 test, 18 gen. Ещё 51 файл исключён как false positive (import без использования в коде). Top-5 методов в use-site файлах: `get(int)` (253), `set(int)` (243), `BitSet()` (168), `BitSet(int)` (119), `nextSetBit()` (67). 23 impl-файла фиксируют повторяющиеся gaps: immutability (5 реализаций), iteration/Iterable (4), serialization (3), compressed storage (3), fluent/convenience API (3).

## Входные данные

- [`bitset-research/step-04a-repo-selection.md`](step-04a-repo-selection.md) — список репозиториев
- [`bitset-research/step-03c-analysis.md`](step-03c-analysis.md) — словарь нормализации (раздел 2.2)

## Методология

1. **Обнаружение:** основной проход `rg -l "import java.util.BitSet" --type java --type kotlin`; дополнительный проход для wildcard-импортов: файлы с `import java.util.*` и `BitSet` в теле кода. Поиск по каждому клону в `/Users/dmitry.nekrasov/dev/repos/for-bitset-research/`.
2. **Фильтрация false positives:** файлы, где `BitSet` встречается только в import-строках и комментариях, исключены (28 elasticsearch generated-src, 20 checkstyle test resources, 1 calcite, 1 h2database, 1 hibernate-orm).
3. **Классификация** (приоритет: gen > test > impl > use):
   - `gen`: путь содержит `/generated/`, `/generated-src/`; или первые 20 строк содержат `@Generated`, `DO NOT EDIT`
   - `test`: путь содержит `/test/`, `/tests/`, `/testFixtures/`, `/jmh/`, `/benchmark/`; или имя файла оканчивается на `Test.java`/`Tests.java`
   - `impl`: файл определяет класс с `*BitSet*` в имени или `extends BitSet`
   - `use`: все остальные
4. **Извлечение методов** (для `use` и `impl` файлов): regex-паттерны по телу файла (без import/comments), нормализация по словарю step-03c.
5. **Контекст:** первая строка Javadoc класса или объявление класса.

### Ограничения методологии

- **Disambiguation `set(int,bool)` vs `set(from,to)`:** без type-info различить вызовы `set(index, booleanVar)` от `set(fromIndex, toIndex)` невозможно. Эвристика: литеральные `true`/`false` и ~20 типичных boolean-имён → `set(int,bool)`; числовые литералы и арифметические выражения → `set(from,to)`; прочие идентификаторы → `set(from,to)` (возможно завышение `set(from,to)` на ≤5 файлов).
- **Ambiguous `get(int)` / `size()` / `isEmpty()`:** в файлах, где BitSet-переменная используется рядом с `Map`, `List` и другими типами, regex может ошибочно приписать `get()` или `size()` к BitSet. Для `get(int)` и `size()` возможно завышение на 5–10%.
- **Pass-through файлы:** файлы, где `BitSet` фигурирует только как тип параметра в сигнатуре метода (напр., listener interfaces), классифицируются как `use` с пометкой «(тип в сигнатуре)» и пустым списком методов.

### Примечания к отдельным репозиториям

- **google/guava**: содержит зеркальные деревья `android/guava/` и `guava/` (Android-compatible и Java 8+ версии). Обе версии каталогизированы; при агрегации учитывать двойной счёт.
- **apache/lucene**: имеет собственный `org.apache.lucene.util.BitSet` (abstract class, ~64 файла) и `FixedBitSet` (~140 файлов). В scope шага 4b входят только файлы с `import java.util.BitSet` (32 файла).
- **RoaringBitmap/RoaringBitmap**: репозиторий является реализацией compressed bitmap. Большинство файлов с j.u.BitSet — тесты, использующие его как reference oracle.
- **apache/hive**: ~350 Thrift-generated файлов упоминают `BitSet` через FQN без import — исключены поисковым фильтром.

## Словарь нормализации

| Raw | Normalized |
|---|---|
| `new BitSet()` | `BitSet()` |
| `new BitSet(N)` / `BitSet(size)` | `BitSet(int)` |
| `BitSet.valueOf(...)` | `valueOf(...)` (overload-agnostic: `long[]`, `byte[]`, `ByteBuffer`, `LongBuffer`) |
| `.set(int, boolean)` | `set(int,bool)` |
| `.isEmpty` (Kotlin property) | `isEmpty()` |
| `.size` (Kotlin property) | `size()` |
| Прочие методы | as-is |

---

## 1. antlr/antlr4

**Домен:** Compilers / parsers / JVM

### Каталог файлов

| # | Путь | Cls | Методы / Описание | Контекст |
|---|---|---|---|---|
| 1 | `runtime/Java/src/org/antlr/v4/runtime/ANTLRErrorListener.java` | use | (тип в сигнатуре) | Upon syntax error, notify any interested parties. This is not how to |
| 2 | `runtime/Java/src/org/antlr/v4/runtime/BaseErrorListener.java` | use | (тип в сигнатуре) | Provides an empty default implementation of {@link ANTLRErrorListener}. The |
| 3 | `runtime/Java/src/org/antlr/v4/runtime/DiagnosticErrorListener.java` | use | BitSet(), set(int) | This implementation of {@link ANTLRErrorListener} can be used to identify |
| 4 | `runtime/Java/src/org/antlr/v4/runtime/ProxyErrorListener.java` | use | (тип в сигнатуре) | This implementation of {@link ANTLRErrorListener} dispatches all calls to a |
| 5 | `runtime/Java/src/org/antlr/v4/runtime/atn/ATNConfigSet.java` | use | BitSet(), set(int) | Specialized {@link Set}{@code <}{@link ATNConfig}{@code >} that can track |
| 6 | `runtime/Java/src/org/antlr/v4/runtime/atn/AmbiguityInfo.java` | use | (тип в сигнатуре) | This class represents profiling event information for an ambiguity |
| 7 | `runtime/Java/src/org/antlr/v4/runtime/atn/LL1Analyzer.java` | use | BitSet(), get(int), set(int), clear(int) | Calculates the SLL(1) expected lookahead set for each outgoing transition |
| 8 | `runtime/Java/src/org/antlr/v4/runtime/atn/ParserATNSimulator.java` | use | BitSet(), get(int), cardinality(), nextSetBit() | The embodiment of the adaptive LL(*), ALL(*), parsing strategy |
| 9 | `runtime/Java/src/org/antlr/v4/runtime/atn/PredictionMode.java` | use | BitSet(), get(int), set(int), or(), nextSetBit(), cardinality(), equals() | This enumeration defines the prediction modes available in ANTLR 4 along with |
| 10 | `runtime/Java/src/org/antlr/v4/runtime/atn/ProfilingATNSimulator.java` | use | nextSetBit() | @since 4.3 |
| 11 | `runtime/Java/src/org/antlr/v4/runtime/misc/Utils.java` | use | nextSetBit() | public class Utils |
| 12 | `tool-testsuite/test/org/antlr/v4/test/tool/TestPerformance.java` | test |  | Parse all java files under this package within the JDK_SOURCE_ROOT |
| 13 | `tool/src/org/antlr/v4/tool/GrammarParserInterpreter.java` | use | BitSet(int), get(int), set(int), nextSetBit(), size() | public class GrammarParserInterpreter |

### Частотная таблица (use-site файлов)

| Метод | Файлов |
|---|---:|
| `BitSet()` | 5 |
| `BitSet(int)` | 1 |
| `get(int)` | 4 |
| `set(int)` | 5 |
| `clear(int)` | 1 |
| `or()` | 1 |
| `nextSetBit()` | 5 |
| `cardinality()` | 2 |
| `size()` | 1 |
| `equals()` | 1 |

**Итого:** 13 файлов (12 use, 1 test)

---

## 2. oracle/graal

**Домен:** Compilers / parsers / JVM

### Каталог файлов

| # | Путь | Cls | Методы / Описание | Контекст |
|---|---|---|---|---|
| 1 | `compiler/src/jdk.graal.compiler.test/src/jdk/graal/compiler/hotspot/amd64/test/StubAVXTest.java` | test |  | public class StubAVXTest |
| 2 | `compiler/src/jdk.graal.compiler.test/src/jdk/graal/compiler/hotspot/replaycomp/test/RecordedOperationPersistenceTest.java` | test |  | Tests the JSON serialization and deserialization performed by |
| 3 | `compiler/src/jdk.graal.compiler/src/jdk/graal/compiler/core/common/alloc/BasicBlockOrderUtils.java` | use | BitSet(int), get(int), set(int) | The initial capacities of the worklists used for iteratively finding the bloc... |
| 4 | `compiler/src/jdk.graal.compiler/src/jdk/graal/compiler/core/common/alloc/DefaultCodeEmissionOrder.java` | use | BitSet(int) | Computes an ordering of the blocks that can be used by the machine code gener... |
| 5 | `compiler/src/jdk.graal.compiler/src/jdk/graal/compiler/core/common/alloc/LinearScanOrder.java` | use | BitSet(int), clear(int) | Computes an ordering of the blocks that can be used by the linear scan regist... |
| 6 | `compiler/src/jdk.graal.compiler/src/jdk/graal/compiler/core/common/cfg/BasicBlockSet.java` | use | BitSet(int), get(int), set(int) | public class BasicBlockSet |
| 7 | `compiler/src/jdk.graal.compiler/src/jdk/graal/compiler/core/common/cfg/DominatorOptimizationProblem.java` | use | BitSet(int), get(int), set(int) | This class represents a dominator tree problem, i.e. a problem which can be s... |
| 8 | `compiler/src/jdk.graal.compiler/src/jdk/graal/compiler/core/common/util/BitMap2D.java` | use | BitSet(int), get(from,to), set(from,to), clear() | This class implements a two-dimensional bitmap |
| 9 | `compiler/src/jdk.graal.compiler/src/jdk/graal/compiler/hotspot/HotSpotForeignCallLinkage.java` | use | BitSet(), set(int), nextSetBit(), cardinality(), toLongArray(), valueOf(...) | The details required to link a HotSpot runtime or stub call |
| 10 | `compiler/src/jdk.graal.compiler/src/jdk/graal/compiler/hotspot/HotSpotReplacementsImpl.java` | use | (тип в сигнатуре) | Filters certain method substitutions based on whether there is underlying har... |
| 11 | `compiler/src/jdk.graal.compiler/src/jdk/graal/compiler/hotspot/SymbolicSnippetEncoder.java` | use | (тип в сигнатуре) | This class performs graph encoding using {@link GraphEncoder} but also conver... |
| 12 | `compiler/src/jdk.graal.compiler/src/jdk/graal/compiler/hotspot/aarch64/AArch64HotSpotRegisterAllocationConfig.java` | use | BitSet(int), get(int), set(int), clear(int), size() | Excluding r27 is a temporary solution until we exclude r27 unconditionally at |
| 13 | `compiler/src/jdk.graal.compiler/src/jdk/graal/compiler/hotspot/amd64/AMD64HotSpotRegisterAllocationConfig.java` | use | BitSet(int), get(int), set(int), clear(int), size() | Specify priority of register selection within phases of register allocation. ... |
| 14 | `compiler/src/jdk.graal.compiler/src/jdk/graal/compiler/hotspot/phases/OnStackReplacementPhase.java` | use | get(int) | Generates a speculative type check on {@code osrLocal} for {@code narrowedStamp} |
| 15 | `compiler/src/jdk.graal.compiler/src/jdk/graal/compiler/hotspot/replaycomp/RecordedOperationPersistence.java` | impl | JSON-сериализация BitSet как long[] массива для replay compilation; нет встроенной JSON serialization | Provides functionality for persisting and loading recorded compilation units |
| 16 | `compiler/src/jdk.graal.compiler/src/jdk/graal/compiler/hotspot/replaycomp/proxy/HotSpotResolvedJavaMethodProxy.java` | use | (тип в сигнатуре) | final class HotSpotResolvedJavaMethodProxy |
| 17 | `compiler/src/jdk.graal.compiler/src/jdk/graal/compiler/hotspot/stubs/SnippetStub.java` | use | (тип в сигнатуре) | Base class for a stub defined by a snippet |
| 18 | `compiler/src/jdk.graal.compiler/src/jdk/graal/compiler/java/BciBlockMapping.java` | use | BitSet(), get(int), set(int), clear(), clear(int), or(), andNot(), nextSetBit(), previousSetBit(), isEmpty(), length(), clone() | Builds a mapping between bytecodes and basic blocks and builds a conservative... |
| 19 | `compiler/src/jdk.graal.compiler/src/jdk/graal/compiler/java/BytecodeParser.java` | use | BitSet(), get(int), set(int), andNot(), nextSetBit(), isEmpty(), cardinality(), clone() | The {@code GraphBuilder} class parses the bytecode of a method and builds the... |
| 20 | `compiler/src/jdk.graal.compiler/src/jdk/graal/compiler/java/LargeLocalLiveness.java` | use | BitSet(int), clear(), or(), andNot(), nextSetBit() | final class LargeLocalLiveness |
| 21 | `compiler/src/jdk.graal.compiler/src/jdk/graal/compiler/java/LocalLiveness.java` | use | BitSet(), or(), nextSetBit() | Encapsulates the liveness calculation, so that subclasses for locals &le; 64 ... |
| 22 | `compiler/src/jdk.graal.compiler/src/jdk/graal/compiler/java/SmallLocalLiveness.java` | use | nextSetBit() | final class SmallLocalLiveness |
| 23 | `compiler/src/jdk.graal.compiler/src/jdk/graal/compiler/lir/alloc/lsra/LinearScanResolveDataFlowPhase.java` | use | BitSet(int), get(int), set(int), clear(), or() | Phase 6: resolve data flow |
| 24 | `compiler/src/jdk.graal.compiler/src/jdk/graal/compiler/lir/amd64/AMD64VZeroUpper.java` | use | BitSet(), get(int), set(int), equals() | vzeroupper is essential to avoid performance penalty during SSE-AVX transitio... |
| 25 | `compiler/src/jdk.graal.compiler/src/jdk/graal/compiler/lir/constopt/ConstantLoadOptimization.java` | use | BitSet(), get(int), set(int) | This optimization tries to improve the handling of constants by replacing a s... |
| 26 | `compiler/src/jdk.graal.compiler/src/jdk/graal/compiler/lir/constopt/ConstantTreeAnalyzer.java` | use | BitSet(int), get(int), set(int), size() | Analyzes a {@link ConstantTree} and marks potential materialization positions |
| 27 | `compiler/src/jdk.graal.compiler/src/jdk/graal/compiler/lir/dfa/UniqueWorkList.java` | use | BitSet(int), get(int) | Ensures that an element is only in the worklist once |
| 28 | `compiler/src/jdk.graal.compiler/src/jdk/graal/compiler/lir/stackslotalloc/FixPointIntervalBuilder.java` | use | BitSet(int), get(int), set(int), clear(int), or(), nextSetBit(), clone(), equals() | Calculates the stack intervals using a worklist-based backwards data-flow ana... |
| 29 | `compiler/src/jdk.graal.compiler/src/jdk/graal/compiler/nodes/GraphDecoder.java` | use | BitSet(int), get(int), set(int), set(from,to), clear(int), nextSetBit(), isEmpty() | Decoder for {@link EncodedGraph encoded graphs} produced by {@link GraphEncod... |
| 30 | `compiler/src/jdk.graal.compiler/src/jdk/graal/compiler/nodes/spi/DelegatingReplacements.java` | use | (тип в сигнатуре) | A convenience class for overriding just a portion of the Replacements API |
| 31 | `compiler/src/jdk.graal.compiler/src/jdk/graal/compiler/nodes/spi/Replacements.java` | use | (тип в сигнатуре) | Interface for managing replacements |
| 32 | `compiler/src/jdk.graal.compiler/src/jdk/graal/compiler/nodes/spi/SnippetParameterInfo.java` | use | BitSet(int), set(int) | Encodes info about a snippet's parameters derived from annotations such as |
| 33 | `compiler/src/jdk.graal.compiler/src/jdk/graal/compiler/phases/common/InsertProxyPhase.java` | use | BitSet(), get(int), set(int) | Phase that inserts proxies after partial evaluation. Performing the proxy ins... |
| 34 | `compiler/src/jdk.graal.compiler/src/jdk/graal/compiler/phases/common/inlining/walker/CallsiteHolderExplorable.java` | use | get(int), isEmpty() | <p> |
| 35 | `compiler/src/jdk.graal.compiler/src/jdk/graal/compiler/phases/common/inlining/walker/InliningData.java` | use | BitSet(), set(int), toString() | <p> |
| 36 | `compiler/src/jdk.graal.compiler/src/jdk/graal/compiler/phases/common/inlining/walker/MethodInvocation.java` | use | (тип в сигнатуре) | <p> |
| 37 | `compiler/src/jdk.graal.compiler/src/jdk/graal/compiler/replacements/DefaultJavaLoweringProvider.java` | use | BitSet(), get(int), set(int) | VM-independent lowerings for standard Java nodes. VM-specific methods are abs... |
| 38 | `compiler/src/jdk.graal.compiler/src/jdk/graal/compiler/replacements/NonNullParameterPlugin.java` | use | get(int) | A {@link ParameterPlugin} that sets non-null stamps for parameters annotated ... |
| 39 | `compiler/src/jdk.graal.compiler/src/jdk/graal/compiler/replacements/ReplacementsImpl.java` | use | isEmpty() | The preprocessed replacement graphs. This is keyed by a pair of a method and ... |
| 40 | `compiler/src/jdk.graal.compiler/src/jdk/graal/compiler/replacements/SnippetTemplate.java` | use | BitSet(int), set(int) | A snippet template is a graph created by parsing a snippet method and then sp... |
| 41 | `compiler/src/jdk.graal.compiler/src/jdk/graal/compiler/virtual/phases/ea/PartialEscapeClosure.java` | use | BitSet(int), get(int), set(int) | Nodes with inputs that were modified during analysis are marked in this bitse... |
| 42 | `espresso-compiler-stub/src/com.oracle.truffle.espresso.graal/src/com/oracle/truffle/espresso/graal/DummyReplacements.java` | use | (тип в сигнатуре) | final class DummyReplacements |
| 43 | `espresso/src/com.oracle.truffle.espresso.libjavavm/src/com/oracle/truffle/espresso/libjavavm/Arguments.java` | use | get(int) | final class Arguments |
| 44 | `espresso/src/com.oracle.truffle.espresso.libjavavm/src/com/oracle/truffle/espresso/libjavavm/LibEspresso.java` | use | BitSet(), set(int) | final class LibEspresso |
| 45 | `espresso/src/com.oracle.truffle.espresso/src/com/oracle/truffle/espresso/analysis/BlockIterator.java` | use | BitSet(int), get(int), set(int), clear(int) | Breadth-first iteration over a graph's blocks |
| 46 | `espresso/src/com.oracle.truffle.espresso/src/com/oracle/truffle/espresso/analysis/GraphBuilder.java` | use | BitSet(), BitSet(int), get(int), set(int) | Analyses control flow of the byte code to identify every start of a basic block |
| 47 | `espresso/src/com.oracle.truffle.espresso/src/com/oracle/truffle/espresso/analysis/Util.java` | impl | mergeBitSets (OR-reduce) и Iterable<Integer> итераторы по set/unset битам; нет bulk-merge и iteration API | final class Util |
| 48 | `espresso/src/com.oracle.truffle.espresso/src/com/oracle/truffle/espresso/analysis/frame/FrameAnalysis.java` | use | BitSet(int), get(int), set(int), set(from,to), set(from,to,bool), clear(int) | Statically analyses bytecodes to produce a {@link EspressoFrameDescriptor fra... |
| 49 | `espresso/src/com.oracle.truffle.espresso/src/com/oracle/truffle/espresso/analysis/graph/EspressoExecutionGraph.java` | use | BitSet(int), get(int), set(int) | final class EspressoExecutionGraph |
| 50 | `espresso/src/com.oracle.truffle.espresso/src/com/oracle/truffle/espresso/analysis/liveness/BlockBoundaryFinder.java` | use | BitSet(int), get(int), set(int) | Does a single pass over all blocks in order to find the set of local variable... |
| 51 | `espresso/src/com.oracle.truffle.espresso/src/com/oracle/truffle/espresso/analysis/liveness/BlockBoundaryResult.java` | use | (тип в сигнатуре) | public interface BlockBoundaryResult |
| 52 | `espresso/src/com.oracle.truffle.espresso/src/com/oracle/truffle/espresso/analysis/liveness/LivenessAnalysis.java` | use | BitSet(int), get(int), set(int), clear(int), or(), andNot(), clone() | final class LivenessAnalysis |
| 53 | `espresso/src/com.oracle.truffle.espresso/src/com/oracle/truffle/espresso/analysis/liveness/LoopPropagatorClosure.java` | use | BitSet(int), get(int), set(int), clear(), clear(int), or(), andNot(), isEmpty(), clone() | Glues together loop entries and loop ends by forcing loop ends to have an end... |
| 54 | `sdk/src/org.graalvm.launcher/src/org/graalvm/launcher/Launcher.java` | use | BitSet(), get(int), set(int), set(from,to) | Default option description indentation |
| 55 | `sdk/src/org.graalvm.polyglot.tck/src/org/graalvm/polyglot/tck/TypeDescriptor.java` | use | BitSet(), get(int), set(int), isEmpty() | Represents a type of a polyglot value. Types include primitive types, null ty... |
| 56 | `substratevm/src/com.oracle.graal.pointsto/src/com/oracle/graal/pointsto/AnalysisPolicy.java` | use | (тип в сигнатуре) | Specifies if this policy models constants objects context sensitively, i.e., ... |
| 57 | `substratevm/src/com.oracle.graal.pointsto/src/com/oracle/graal/pointsto/flow/context/bytecode/BytecodeSensitiveAnalysisPolicy.java` | use | and(), or(), andNot(), cardinality() | Wraps the analysis object corresponding to a clone site for a given context i... |
| 58 | `substratevm/src/com.oracle.graal.pointsto/src/com/oracle/graal/pointsto/flow/context/bytecode/ContextSensitiveMultiTypeState.java` | use | get(int), equals() | Returns an array of all type ids from the {@link #objects} array. This mitiga... |
| 59 | `substratevm/src/com.oracle.graal.pointsto/src/com/oracle/graal/pointsto/typestate/DefaultAnalysisPolicy.java` | use | set(from,to), clear(int), and(), or(), andNot(), nextSetBit(), cardinality() | This class implements the default, context-insensitive, static analysis policy |
| 60 | `substratevm/src/com.oracle.graal.pointsto/src/com/oracle/graal/pointsto/typestate/MultiTypeStateWithBitSet.java` | impl | BitSet + cached cardinality + type-state metadata; j.u.BitSet пересчитывает cardinality() при каждом вызове | Keep a bit set for types to easily answer queries like contains type or types... |
| 61 | `substratevm/src/com.oracle.graal.pointsto/src/com/oracle/graal/pointsto/typestate/TypeStateUtils.java` | use | BitSet(int), set(int), clear(int), and(), or(), andNot(), nextSetBit(), cardinality(), size(), length(), clone() | This method gives access to the java.lang.BitSet's array of bytes. We need th... |
| 62 | `substratevm/src/com.oracle.svm.core.foreign/src/com/oracle/svm/core/foreign/TrampolineSet.java` | use | BitSet(int), get(int), set(int), clear(), isEmpty() | A set of trampolines that can be assigned to specific upcall stubs with speci... |
| 63 | `substratevm/src/com.oracle.svm.core/src/com/oracle/svm/core/code/CodeInfoEncoder.java` | use | BitSet(), get(int), set(int) | Encapsulates {@link FrequencyEncoder}s for values that are referenced by an i... |
| 64 | `substratevm/src/com.oracle.svm.core/src/com/oracle/svm/core/code/FrameInfoEncoder.java` | use | BitSet(), get(int), set(int), length() | Hook for when a frame is registered for encoding |
| 65 | `substratevm/src/com.oracle.svm.core/src/com/oracle/svm/core/graal/meta/SubstrateReplacements.java` | use | (тип в сигнатуре) | The replacements implementation for the compiler at runtime. All snippets and... |
| 66 | `substratevm/src/com.oracle.svm.core/src/com/oracle/svm/core/heap/Pod.java` | use | BitSet(), get(int), set(int), set(from,to), nextSetBit(), nextClearBit(), isEmpty(), equals() | A structure of fields, including object references, that is {@linkplain Build... |
| 67 | `substratevm/src/com.oracle.svm.core/src/com/oracle/svm/core/heap/SubstrateReferenceMap.java` | use | BitSet(), get(int), set(int), nextSetBit(), previousSetBit(), isEmpty(), toLongArray(), valueOf(...), stream() | Stores the reference map data. 3 bits are currently required per entry: the f... |
| 68 | `substratevm/src/com.oracle.svm.hosted/src/com/oracle/svm/hosted/code/SubstrateGraphMaker.java` | use | (тип в сигнатуре) | public class SubstrateGraphMaker |
| 69 | `substratevm/src/com.oracle.svm.hosted/src/com/oracle/svm/hosted/dashboard/PointsToBreakdown.java` | use | BitSet(), get(int), set(int), stream() | Creates a JSON representation of the pointsto graph, in the format expected b... |
| 70 | `substratevm/src/com.oracle.svm.hosted/src/com/oracle/svm/hosted/dynamicaccessinference/dataflow/ForwardDataFlowAnalyzer.java` | use | BitSet(), nextSetBit() | Abstract bytecode forward data-flow analyzer. Abstract program states, repres... |
| 71 | `substratevm/src/com.oracle.svm.hosted/src/com/oracle/svm/hosted/imagelayer/ImageLayerSectionFeature.java` | use | isEmpty(), toByteArray() | Creates a {@linkplain ImageLayerSection section} with information specific to... |
| 72 | `substratevm/src/com.oracle.svm.hosted/src/com/oracle/svm/hosted/imagelayer/LayeredDispatchTableFeature.java` | use | BitSet(), set(int) | Tracks the state of type dispatch tables across layers. Across layers, this i... |
| 73 | `substratevm/src/com.oracle.svm.hosted/src/com/oracle/svm/hosted/meta/TypeCheckBuilder.java` | use | BitSet(), get(int), set(int), and(), or(), andNot(), nextSetBit(), isEmpty(), cardinality(), intersects(), clone(), equals() | This class assigns each type an id, determines stamp metadata, and generates ... |
| 74 | `substratevm/src/com.oracle.svm.hosted/src/com/oracle/svm/hosted/meta/UniverseBuilder.java` | use | BitSet(), set(from,to), nextSetBit(), nextClearBit(), previousSetBit(), length(), clone() | This step is single threaded, i.e., all the maps are modified only by a singl... |
| 75 | `substratevm/src/com.oracle.svm.hosted/src/com/oracle/svm/hosted/meta/VTableBuilder.java` | use | BitSet(), get(int), or(), nextClearBit() | We are allowed to filter vtables as long as we know the type layout does not ... |
| 76 | `substratevm/src/com.oracle.svm.interpreter/src/com/oracle/svm/interpreter/ristretto/meta/RistrettoReplacements.java` | use | (тип в сигнатуре) | The replacements implementation for the Ristretto JIT compiler at runtime. Al... |
| 77 | `sulong/projects/com.oracle.truffle.llvm.parser/src/com/oracle/truffle/llvm/parser/LLVMLivenessAnalysis.java` | use | BitSet(int), get(int), set(int), set(from,to), clear(), clear(int), or(), andNot(), nextSetBit(), size() | Holds the information when a certain value can be invalidated. The nullableWi... |
| 78 | `sulong/projects/com.oracle.truffle.llvm.parser/src/com/oracle/truffle/llvm/parser/LazyToTruffleConverterImpl.java` | use | nextSetBit() | Check whether the function parameter has an LLVM byval attribute attached to ... |
| 79 | `sulong/projects/com.oracle.truffle.llvm.parser/src/com/oracle/truffle/llvm/parser/ValueList.java` | use | BitSet(), get(int), set(int), set(int,bool) | Produce a new unique value used as placeholder for a forward referenced symbol |
| 80 | `sulong/projects/com.oracle.truffle.llvm.runtime/src/com/oracle/truffle/llvm/runtime/LLVMIVarBitLarge.java` | use | BitSet(int), set(int), isEmpty(), toByteArray() | Implementation of variable-width integers with > 64 bits in size |
| 81 | `sulong/projects/com.oracle.truffle.llvm/src/com/oracle/truffle/llvm/initialization/LoadModulesNode.java` | use | BitSet(int), get(int), set(int), clear() | The {@link LoadModulesNode} initialise the library. This involves building th... |
| 82 | `tools/src/com.oracle.truffle.tools.agentscript/src/com/oracle/truffle/tools/agentscript/impl/InsightInstrument.java` | use | BitSet(), set(int), clear(int), nextClearBit(), length() | public class InsightInstrument |
| 83 | `truffle/src/com.oracle.truffle.api.dsl.test/src/com/oracle/truffle/api/dsl/test/MergeSpecializationsTest.java` | test |  | public class MergeSpecializationsTest |
| 84 | `truffle/src/com.oracle.truffle.api.dsl.test/src/com/oracle/truffle/api/dsl/test/StateBitTest.java` | test |  | public class StateBitTest |
| 85 | `truffle/src/com.oracle.truffle.api.dsl/src/com/oracle/truffle/api/dsl/SpecializationStatistics.java` | use | BitSet(), get(int), set(int), nextSetBit() | Represents a specialization statistics utiltiy that can be {@link #enter() en... |
| 86 | `truffle/src/com.oracle.truffle.api.strings/src/com/oracle/truffle/api/strings/TruffleString.java` | use | BitSet(int), get(int), set(int) | Represents a primitive String type, which can be reused across languages. Lan... |
| 87 | `truffle/src/com.oracle.truffle.api.test/src/com/oracle/truffle/api/test/utilities/FinalBitSetTest.java` | test |  | public class FinalBitSetTest |
| 88 | `truffle/src/com.oracle.truffle.api.utilities/src/com/oracle/truffle/api/utilities/FinalBitSet.java` | impl | Read-only BitSet с @CompilationFinal long[] для JIT constant-folding; j.u.BitSet мутабелен и непрозрачен для partial evaluation | Read-only bitset designed for partial evaluation. The implementation is parti... |
| 89 | `truffle/src/com.oracle.truffle.api/src/com/oracle/truffle/api/frame/FrameDescriptor.java` | use | BitSet(), get(int), set(int), clear(int), or() | Descriptor of the slots of frame objects. Multiple frame instances are associ... |
| 90 | `truffle/src/com.oracle.truffle.dsl.processor/src/com/oracle/truffle/dsl/processor/library/LibraryGenerator.java` | use | BitSet(int) | public class LibraryGenerator |
| 91 | `truffle/src/com.oracle.truffle.polyglot/src/com/oracle/truffle/polyglot/PolyglotContextImpl.java` | use | BitSet(int), get(int), set(int), cardinality() | Claims a sharing layer for a context. This typically happens at when the firs... |
| 92 | `truffle/src/com.oracle.truffle.polyglot/src/com/oracle/truffle/polyglot/PolyglotThreadInfo.java` | use | BitSet(int), get(int), set(int), clear(int), cardinality() | Only true if the thread was created "inside" exitContext, i.e. created from t... |
| 93 | `visualizer/C1Visualizer/TextEditor/src/main/java/at/ssw/visualizer/texteditor/model/Scanner.java` | use | BitSet(), get(int), set(int) | The implementing class must specify the used <code> TokenID</code>s and |
| 94 | `visualizer/IdealGraphVisualizer/Data/src/main/java/org/graalvm/visualizer/data/serialization/lazy/GraphMetadata.java` | use | BitSet(), set(int) | Metadata distilled during scanning the input data, for use before the graph |
| 95 | `visualizer/IdealGraphVisualizer/Data/src/main/java/org/graalvm/visualizer/data/serialization/lazy/LazyGraph.java` | use | get(int), isEmpty(), nextSetBit() | Implementation which loads the data lazily |
| 96 | `visualizer/IdealGraphVisualizer/Data/src/main/java/org/graalvm/visualizer/data/serialization/lazy/StreamPool.java` | use | BitSet(), get(int), set(int), clear() | Stream constant pool. Performs a snapshot before data is overwritten in the p... |
| 97 | `visualizer/IdealGraphVisualizer/GraphSearch/src/main/java/org/graalvm/visualizer/search/ui/actions/GraphContextAction.java` | use | BitSet(), get(int), set(int) | @author sdedic |
| 98 | `wasm/src/org.graalvm.wasm/src/org/graalvm/wasm/GlobalRegistry.java` | use | BitSet(int), get(int), set(int,bool) | The global registry holds the global values in the WebAssembly module instance |
| 99 | `wasm/src/org.graalvm.wasm/src/org/graalvm/wasm/parser/validation/BlockFrame.java` | use | BitSet(int), set(int), clone() | Representation of a wasm block during module validation |
| 100 | `wasm/src/org.graalvm.wasm/src/org/graalvm/wasm/parser/validation/ControlFrame.java` | use | get(int), set(int), clone() | Represents the scope of a block structure during module validation |
| 101 | `wasm/src/org.graalvm.wasm/src/org/graalvm/wasm/parser/validation/IfFrame.java` | use | clone() | Representation of a wasm if and else block during module validation |
| 102 | `wasm/src/org.graalvm.wasm/src/org/graalvm/wasm/parser/validation/LoopFrame.java` | use | clone() | Representation of a wasm loop during module validation |
| 103 | `web-image/src/com.oracle.svm.hosted.webimage/src/com/oracle/svm/hosted/webimage/wasm/ast/ActiveData.java` | use | BitSet(int), get(int), set(int), size() | Manages active data segments of a {@link WasmModule} |

### Частотная таблица (use-site файлов)

| Метод | Файлов |
|---|---:|
| `BitSet()` | 31 |
| `BitSet(int)` | 36 |
| `get(int)` | 54 |
| `get(from,to)` | 1 |
| `set(int)` | 56 |
| `set(int,bool)` | 2 |
| `set(from,to)` | 8 |
| `set(from,to,bool)` | 1 |
| `clear()` | 9 |
| `clear(int)` | 16 |
| `and()` | 4 |
| `or()` | 14 |
| `andNot()` | 10 |
| `nextSetBit()` | 19 |
| `nextClearBit()` | 4 |
| `previousSetBit()` | 3 |
| `isEmpty()` | 14 |
| `cardinality()` | 8 |
| `size()` | 6 |
| `length()` | 5 |
| `intersects()` | 1 |
| `toByteArray()` | 2 |
| `toLongArray()` | 2 |
| `valueOf(...)` | 2 |
| `stream()` | 2 |
| `clone()` | 12 |
| `equals()` | 5 |
| `toString()` | 1 |

**Итого:** 103 файлов (94 use, 4 impl, 5 test)

---

## 3. apache/lucene

**Домен:** Search / indexing

### Каталог файлов

| # | Путь | Cls | Методы / Описание | Контекст |
|---|---|---|---|---|
| 1 | `lucene/analysis/common/src/java/org/apache/lucene/analysis/hunspell/NGramFragmentChecker.java` | use | BitSet(int), set(from,to,bool), cardinality(), size() | A {@link FragmentChecker} based on all character n-grams possible in a certai... |
| 2 | `lucene/analysis/common/src/java/org/apache/lucene/analysis/in/IndicNormalizer.java` | use | BitSet(int), get(int), set(int) | Normalizes the Unicode representation of text in Indian languages |
| 3 | `lucene/codecs/src/java/org/apache/lucene/codecs/memory/FSTTermsReader.java` | use | BitSet(), get(int), set(int) | FST-based terms dictionary reader |
| 4 | `lucene/codecs/src/java/org/apache/lucene/codecs/simpletext/SimpleTextLiveDocsFormat.java` | use | BitSet(int), get(int), set(int), length() | reads/writes plaintext live docs |
| 5 | `lucene/core/src/java/org/apache/lucene/util/automaton/Automaton.java` | use | BitSet(int), get(int), set(from,to), nextSetBit(), size() | Represents an automaton and all its states and transitions. States are intege... |
| 6 | `lucene/core/src/java/org/apache/lucene/util/automaton/FiniteStringsIterator.java` | use | BitSet(int), get(int), set(int), clear(int) | Iterates all accepted strings |
| 7 | `lucene/core/src/java/org/apache/lucene/util/automaton/Operations.java` | use | BitSet(int), get(int), set(int), clear(int), and(), andNot(), nextSetBit(), isEmpty(), cardinality() | Automata operations |
| 8 | `lucene/core/src/java/org/apache/lucene/util/fst/Util.java` | use | BitSet(), get(int), set(int) | Static helper methods |
| 9 | `lucene/core/src/java/org/apache/lucene/util/graph/GraphTokenStreamFiniteStrings.java` | use | BitSet(int), get(int), set(int) | Consumes a TokenStream and creates an {@link Automaton} where the transition ... |
| 10 | `lucene/core/src/test/org/apache/lucene/index/TestTransactionRollback.java` | test |  | Test class to illustrate using IndexDeletionPolicy to provide multi-level rol... |
| 11 | `lucene/core/src/test/org/apache/lucene/search/TestPointQueries.java` | test |  | public class TestPointQueries |
| 12 | `lucene/core/src/test/org/apache/lucene/util/TestBytesRefHash.java` | test |  | Test method for {@link |
| 13 | `lucene/core/src/test/org/apache/lucene/util/TestFixedBitDocIdSet.java` | test |  | public class TestFixedBitDocIdSet |
| 14 | `lucene/core/src/test/org/apache/lucene/util/TestIntArrayDocIdSet.java` | test |  | public class TestIntArrayDocIdSet |
| 15 | `lucene/core/src/test/org/apache/lucene/util/TestNotDocIdSet.java` | test |  | public class TestNotDocIdSet |
| 16 | `lucene/core/src/test/org/apache/lucene/util/TestRoaringDocIdSet.java` | test |  | public class TestRoaringDocIdSet |
| 17 | `lucene/core/src/test/org/apache/lucene/util/TestSparseFixedBitDocIdSet.java` | test |  | public class TestSparseFixedBitDocIdSet |
| 18 | `lucene/core/src/test/org/apache/lucene/util/automaton/MinimizationOperations.java` | test |  | Operations for minimizing automata |
| 19 | `lucene/core/src/test/org/apache/lucene/util/bkd/TestBKD.java` | test |  | public class TestBKD |
| 20 | `lucene/highlighter/src/test/org/apache/lucene/search/uhighlight/TestUnifiedHighlighterTermVec.java` | test |  | Tests highlighting for matters *expressly* relating to term vectors |
| 21 | `lucene/spatial3d/src/java/org/apache/lucene/spatial3d/geom/GeoConcavePolygon.java` | use | BitSet(), get(int), set(int), size(), equals() | GeoConcavePolygon objects are generic building blocks of more complex structu... |
| 22 | `lucene/spatial3d/src/java/org/apache/lucene/spatial3d/geom/GeoConvexPolygon.java` | use | BitSet(), get(int), set(int), size(), equals() | GeoConvexPolygon objects are generic building blocks of more complex structur... |
| 23 | `lucene/spatial3d/src/java/org/apache/lucene/spatial3d/geom/GeoPolygonFactory.java` | use | BitSet(), BitSet(int), get(int), set(int,bool), set(from,to), size() | Class which constructs a GeoMembershipShape representing an arbitrary polygon |
| 24 | `lucene/spatial3d/src/java/org/apache/lucene/spatial3d/geom/SerializableObject.java` | use | toByteArray(), valueOf(...) | Indicates that a geo3d object can be serialized and deserialized |
| 25 | `lucene/spatial3d/src/test/org/apache/lucene/spatial3d/geom/TestGeoPolygon.java` | test |  | public class TestGeoPolygon |
| 26 | `lucene/test-framework/src/java/org/apache/lucene/tests/geo/BaseGeoPointTestCase.java` | test |  | Abstract class to do basic tests for a geospatial impl (high level fields and... |
| 27 | `lucene/test-framework/src/java/org/apache/lucene/tests/geo/BaseXYPointTestCase.java` | test |  | abstract class BaseXYPointTestCase |
| 28 | `lucene/test-framework/src/java/org/apache/lucene/tests/index/BasePointsFormatTestCase.java` | test |  | Abstract class to do basic tests for a points format. NOTE: This test focuses... |
| 29 | `lucene/test-framework/src/java/org/apache/lucene/tests/search/SearchEquivalenceTestBase.java` | test |  | Simple base class for checking search equivalence. Extend it, and write tests... |
| 30 | `lucene/test-framework/src/java/org/apache/lucene/tests/util/BaseDocIdSetTestCase.java` | test |  | Assert that the content of the {@link DocIdSet} is the same as the content of... |
| 31 | `lucene/test-framework/src/java/org/apache/lucene/tests/util/RamUsageTester.java` | test |  | An accumulator of object references. This class allows for customizing RAM us... |
| 32 | `lucene/test-framework/src/java/org/apache/lucene/tests/util/automaton/AutomatonTestUtil.java` | test |  | Utilities for testing automata |

### Частотная таблица (use-site файлов)

| Метод | Файлов |
|---|---:|
| `BitSet()` | 5 |
| `BitSet(int)` | 8 |
| `get(int)` | 11 |
| `set(int)` | 9 |
| `set(int,bool)` | 1 |
| `set(from,to)` | 2 |
| `set(from,to,bool)` | 1 |
| `clear(int)` | 2 |
| `and()` | 1 |
| `andNot()` | 1 |
| `nextSetBit()` | 2 |
| `isEmpty()` | 1 |
| `cardinality()` | 2 |
| `size()` | 5 |
| `length()` | 1 |
| `toByteArray()` | 1 |
| `valueOf(...)` | 1 |
| `equals()` | 2 |

**Итого:** 32 файлов (13 use, 19 test)

---

## 4. androidx/androidx

**Домен:** Android ecosystem

### Каталог файлов

| # | Путь | Cls | Методы / Описание | Контекст |
|---|---|---|---|---|
| 1 | `core/core-backported-fixes/src/main/java/androidx/core/backported/fixes/SystemPropertyResolver.kt` | use | valueOf(...) | Resolves the status of a [KnownIssue] using `ro.build.backported_fixes.alias_... |
| 2 | `pdf/pdf-ink/src/main/kotlin/androidx/pdf/ink/EditableDocumentViewModel.kt` | use | BitSet() | Fetches annotations from the [PdfAnnotationsManager] for the defined page range |
| 3 | `profileinstaller/profileinstaller/src/main/java/androidx/profileinstaller/ProfileTranscoder.java` | use | valueOf(...) | Transcode (or convert) a binary profile from one format version to another |
| 4 | `recyclerview/recyclerview/src/androidTest/java/androidx/recyclerview/widget/AsyncListUtilLayoutTest.java` | test |  | public class AsyncListUtilLayoutTest |
| 5 | `recyclerview/recyclerview/src/androidTest/java/androidx/recyclerview/widget/GridLayoutManagerBaseConfigSetTest.java` | test |  | public class GridLayoutManagerBaseConfigSetTest |
| 6 | `recyclerview/recyclerview/src/androidTest/java/androidx/recyclerview/widget/StaggeredGridLayoutManagerBaseConfigSetTest.java` | test |  | public class StaggeredGridLayoutManagerBaseConfigSetTest |
| 7 | `recyclerview/recyclerview/src/main/java/androidx/recyclerview/widget/StaggeredGridLayoutManager.java` | use | BitSet(int), get(int), set(int,bool), set(from,to,bool), clear(), clear(int), isEmpty() | A LayoutManager that lays out children in a staggered grid formation |

### Частотная таблица (use-site файлов)

| Метод | Файлов |
|---|---:|
| `BitSet()` | 1 |
| `BitSet(int)` | 1 |
| `get(int)` | 1 |
| `set(int,bool)` | 1 |
| `set(from,to,bool)` | 1 |
| `clear()` | 1 |
| `clear(int)` | 1 |
| `isEmpty()` | 1 |
| `valueOf(...)` | 2 |

**Итого:** 7 файлов (4 use, 3 test)

---

## 5. google/guava

**Домен:** JVM core libraries / utilities

### Каталог файлов

| # | Путь | Cls | Методы / Описание | Контекст |
|---|---|---|---|---|
| 1 | `android/guava-testlib/test/com/google/common/testing/ArbitraryInstancesTest.java` | test |  | Unit test for {@link ArbitraryInstances} |
| 2 | `android/guava-tests/benchmark/com/google/common/base/CharMatcherBenchmark.java` | test |  | Benchmark for the {@link CharMatcher} class |
| 3 | `android/guava-tests/benchmark/com/google/common/base/WhitespaceMatcherBenchmark.java` | test |  | public class WhitespaceMatcherBenchmark |
| 4 | `android/guava-tests/test/com/google/common/base/CharMatcherTest.java` | test |  | Unit test for {@link CharMatcher} |
| 5 | `android/guava/src/com/google/common/base/CharMatcher.java` | impl | BitSetMatcher: адаптер BitSet -> CharMatcher predicate (matches(char)); нет predicate-интерфейса в j.u.BitSet | Determines a true or false value for any Java {@code char} value, just as {@l... |
| 6 | `android/guava/src/com/google/common/base/SmallCharMatcher.java` | use | get(int), set(int), nextSetBit(), cardinality() | An immutable version of CharMatcher for smallish sets of characters that uses... |
| 7 | `android/guava/src/com/google/common/collect/ImmutableMap.java` | use | BitSet(), get(int), set(int), isEmpty(), cardinality() | A {@link Map} whose contents will never change, with many other important pro... |
| 8 | `android/guava/src/com/google/common/collect/Sets.java` | use | BitSet(int), get(int), set(int), set(from,to), clear(from,to), nextSetBit(), nextClearBit(), isEmpty(), size(), clone() | Static utility methods pertaining to {@link Set} instances. Also see this cla... |
| 9 | `guava-testlib/test/com/google/common/testing/ArbitraryInstancesTest.java` | test |  | Unit test for {@link ArbitraryInstances} |
| 10 | `guava-tests/benchmark/com/google/common/base/CharMatcherBenchmark.java` | test |  | Benchmark for the {@link CharMatcher} class |
| 11 | `guava-tests/benchmark/com/google/common/base/WhitespaceMatcherBenchmark.java` | test |  | public class WhitespaceMatcherBenchmark |
| 12 | `guava-tests/test/com/google/common/base/CharMatcherTest.java` | test |  | Unit test for {@link CharMatcher} |
| 13 | `guava/src/com/google/common/base/CharMatcher.java` | impl | BitSetMatcher: адаптер BitSet -> CharMatcher predicate (matches(char)); нет predicate-интерфейса в j.u.BitSet | Determines a true or false value for any Java {@code char} value, just as {@l... |
| 14 | `guava/src/com/google/common/base/SmallCharMatcher.java` | use | get(int), set(int), nextSetBit(), cardinality() | An immutable version of CharMatcher for smallish sets of characters that uses... |
| 15 | `guava/src/com/google/common/collect/ImmutableMap.java` | use | BitSet(), get(int), set(int), isEmpty(), cardinality() | A {@link Map} whose contents will never change, with many other important pro... |
| 16 | `guava/src/com/google/common/collect/Sets.java` | use | BitSet(int), get(int), set(int), set(from,to), clear(from,to), nextSetBit(), nextClearBit(), isEmpty(), size(), clone() | Static utility methods pertaining to {@link Set} instances. Also see this cla... |

### Частотная таблица (use-site файлов)

| Метод | Файлов |
|---|---:|
| `BitSet()` | 2 |
| `BitSet(int)` | 2 |
| `get(int)` | 6 |
| `set(int)` | 6 |
| `set(from,to)` | 2 |
| `clear(from,to)` | 2 |
| `nextSetBit()` | 4 |
| `nextClearBit()` | 2 |
| `isEmpty()` | 4 |
| `cardinality()` | 4 |
| `size()` | 2 |
| `clone()` | 2 |

**Итого:** 16 файлов (6 use, 2 impl, 8 test)

---

## 6. apache/spark

**Домен:** Data processing / pipeline

### Каталог файлов

| # | Путь | Cls | Методы / Описание | Контекст |
|---|---|---|---|---|
| 1 | `core/src/main/java/org/apache/spark/memory/TaskMemoryManager.java` | use | BitSet(int), get(int), set(int), clear(int) | Manages the memory allocated by an individual task |
| 2 | `core/src/test/java/org/apache/spark/unsafe/map/AbstractBytesToBytesMapSuite.java` | test |  | public abstract class AbstractBytesToBytesMapSuite |
| 3 | `sql/hive/src/test/java/org/apache/spark/sql/hive/test/Complex.java` | test |  | This is a fork of Hive 0.13's org/apache/hadoop/hive/serde2/thrift/test/Compl... |

### Частотная таблица (use-site файлов)

| Метод | Файлов |
|---|---:|
| `BitSet(int)` | 1 |
| `get(int)` | 1 |
| `set(int)` | 1 |
| `clear(int)` | 1 |

**Итого:** 3 файлов (1 use, 2 test)

---

## 7. spotbugs/spotbugs

**Домен:** Static analysis / code quality

### Каталог файлов

| # | Путь | Cls | Методы / Описание | Контекст |
|---|---|---|---|---|
| 1 | `spotbugs/src/main/java/edu/umd/cs/findbugs/LocalVariableAnnotation.java` | use | get(int), equals() | Bug annotation class for local variable names |
| 2 | `spotbugs/src/main/java/edu/umd/cs/findbugs/OpcodeStack.java` | use | BitSet(), get(int), set(int), clear(), nextSetBit(), clone() | tracks the types and numbers of objects that are currently on the operand |
| 3 | `spotbugs/src/main/java/edu/umd/cs/findbugs/StackMapAnalyzer.java` | use | BitSet(), set(int) | @author pugh |
| 4 | `spotbugs/src/main/java/edu/umd/cs/findbugs/Tokenizer.java` | use | BitSet(), get(int), set(int), set(from,to) | A simple tokenizer for Java source text. This is not intended to be a |
| 5 | `spotbugs/src/main/java/edu/umd/cs/findbugs/ba/AbstractDominatorsAnalysis.java` | use | BitSet(), get(int), set(int), clear(), and(), or(), equals() | <p>A dataflow analysis to compute dominator relationships between basic blocks |
| 6 | `spotbugs/src/main/java/edu/umd/cs/findbugs/ba/AnalysisContext.java` | use | BitSet(), get(int), set(int,bool) | A context for analysis of a complete project. This serves as the repository |
| 7 | `spotbugs/src/main/java/edu/umd/cs/findbugs/ba/AssertionMethods.java` | use | BitSet(), get(int), set(int) | Mark methodref constant pool entries of methods that are likely to implement |
| 8 | `spotbugs/src/main/java/edu/umd/cs/findbugs/ba/BasicBlock.java` | use | BitSet(), get(int), set(int) | Simple basic block abstraction for BCEL |
| 9 | `spotbugs/src/main/java/edu/umd/cs/findbugs/ba/BetterCFGBuilder2.java` | use | BitSet(), get(int), set(int) | A CFGBuilder that really tries to construct accurate control flow graphs. The |
| 10 | `spotbugs/src/main/java/edu/umd/cs/findbugs/ba/BlockType.java` | impl | BitSet как dataflow lattice value: validity, top/bottom, depth tracking; нет lattice-семантики и metadata fields | Dataflow value representing the current nesting of catch and finally blocks |
| 11 | `spotbugs/src/main/java/edu/umd/cs/findbugs/ba/CFG.java` | use | get(int) | Simple control flow graph abstraction for BCEL |
| 12 | `spotbugs/src/main/java/edu/umd/cs/findbugs/ba/ClassContext.java` | use | BitSet(), get(int), set(int), set(from,to), and(), nextSetBit(), isEmpty() | A ClassContext caches all of the auxiliary objects used to analyze the |
| 13 | `spotbugs/src/main/java/edu/umd/cs/findbugs/ba/Frame.java` | use | BitSet(), set(int) | <p>Generic class for representing a Java stack frame as a dataflow value. A |
| 14 | `spotbugs/src/main/java/edu/umd/cs/findbugs/ba/LiveLocalStoreAnalysis.java` | use | BitSet(), get(int), set(int), clear(), clear(int), or(), nextSetBit(), equals() | Dataflow analysis to find live stores of locals. This is just a backward |
| 15 | `spotbugs/src/main/java/edu/umd/cs/findbugs/ba/LiveLocalStoreDataflow.java` | use | (тип в сигнатуре) | Dataflow class for LiveLocalStoreAnalysis |
| 16 | `spotbugs/src/main/java/edu/umd/cs/findbugs/ba/LockChecker.java` | use | get(int) | Front-end for LockDataflow that can avoid doing unnecessary work (e.g., |
| 17 | `spotbugs/src/main/java/edu/umd/cs/findbugs/ba/MethodBytecodeSet.java` | impl | toString() с маппингом bit positions -> JVM opcode names; нет кастомного domain-specific toString в j.u.BitSet | Class representing the set of opcodes used in a method |
| 18 | `spotbugs/src/main/java/edu/umd/cs/findbugs/ba/PruneUnconditionalExceptionThrowerEdges.java` | use | BitSet(), set(int) | @param xMethod |
| 19 | `spotbugs/src/main/java/edu/umd/cs/findbugs/ba/bcp/PatternMatcher.java` | use | BitSet(), get(int), set(int,bool) | <p>Match a ByteCodePattern against the code of a method, represented by a CFG |
| 20 | `spotbugs/src/main/java/edu/umd/cs/findbugs/ba/deref/UnconditionalValueDerefSet.java` | use | BitSet(), get(int), set(int), clear(), clear(int), and(), or(), isEmpty(), equals() | A set of values unconditionally dereferenced in the future |
| 21 | `spotbugs/src/main/java/edu/umd/cs/findbugs/ba/interproc/ParameterProperty.java` | use | BitSet(), get(int), set(from,to) | Method property recording which parameters are have some property |
| 22 | `spotbugs/src/main/java/edu/umd/cs/findbugs/ba/npe/DerefFinder.java` | use | and(), nextSetBit(), isEmpty() | @author pugh |
| 23 | `spotbugs/src/main/java/edu/umd/cs/findbugs/ba/npe/IsNullValueAnalysis.java` | use | BitSet(), get(int), set(int) | A dataflow analysis to detect potential null pointer dereferences |
| 24 | `spotbugs/src/main/java/edu/umd/cs/findbugs/ba/npe/NullDerefAndRedundantComparisonFinder.java` | use | BitSet(), get(int), set(int), clear(int), or() | A user-friendly front end for finding null pointer dereferences and redundant |
| 25 | `spotbugs/src/main/java/edu/umd/cs/findbugs/ba/npe/UsagesRequiringNonNullValues.java` | use | get(int) | @author pugh |
| 26 | `spotbugs/src/main/java/edu/umd/cs/findbugs/ba/type/ExceptionSet.java` | use | BitSet(), get(int), set(int), clear(), clear(int), or(), equals(), hashCode() | Class for keeping track of exceptions that can be thrown by an instruction |
| 27 | `spotbugs/src/main/java/edu/umd/cs/findbugs/ba/type/TypeFrame.java` | use | BitSet(), get(int), set(from,to), clear(), clear(int), or() | A specialization of {@link Frame} for determining the types of values in the |
| 28 | `spotbugs/src/main/java/edu/umd/cs/findbugs/ba/type/TypeFrameModelingVisitor.java` | use | BitSet(), get(int), set(int) | Visitor to model the effects of bytecode instructions on the types of the |
| 29 | `spotbugs/src/main/java/edu/umd/cs/findbugs/ba/vna/LoadedFieldSet.java` | use | BitSet(), get(int), set(int) | Object which stores which fields are loaded and stored by the instructions in |
| 30 | `spotbugs/src/main/java/edu/umd/cs/findbugs/ba/vna/MergeTree.java` | use | BitSet(), get(int), set(int), or(), nextSetBit() | Data structure to keep track of which input ValueNumbers were combined to |
| 31 | `spotbugs/src/main/java/edu/umd/cs/findbugs/classfile/engine/ClassParserUsingASM.java` | use | BitSet(), set(int) | @author William Pugh |
| 32 | `spotbugs/src/main/java/edu/umd/cs/findbugs/classfile/engine/bcel/FinallyDuplicatesInfoFactory.java` | use | BitSet(), get(int), set(int), clear(int), nextSetBit(), isEmpty(), size(), toString() | @author Tagir Valeev |
| 33 | `spotbugs/src/main/java/edu/umd/cs/findbugs/classfile/engine/bcel/LoadedFieldSetFactory.java` | use | BitSet(), get(int), set(int) | Factory to determine which fields are loaded and stored by the instructions |
| 34 | `spotbugs/src/main/java/edu/umd/cs/findbugs/classfile/engine/bcel/NonImplicitExceptionDominatorsAnalysisFactory.java` | use | (тип в сигнатуре) | Analysis engine to produce NonImplicitExceptionDominatorsAnalysis objects |
| 35 | `spotbugs/src/main/java/edu/umd/cs/findbugs/classfile/engine/bcel/NonImplicitExceptionPostDominatorsAnalysisFactory.java` | use | (тип в сигнатуре) | Analysis engine to produce NonImplicitExceptionPostDominatorsAnalysis objects |
| 36 | `spotbugs/src/main/java/edu/umd/cs/findbugs/classfile/engine/bcel/ValueRangeAnalysisFactory.java` | use | BitSet(), get(int), set(int), set(from,to), or(), isEmpty() | public class ValueRangeAnalysisFactory |
| 37 | `spotbugs/src/main/java/edu/umd/cs/findbugs/detect/BuildUnconditionalParamDerefDatabase.java` | use | BitSet(), set(int), isEmpty() | Build database of unconditionally dereferenced parameters |
| 38 | `spotbugs/src/main/java/edu/umd/cs/findbugs/detect/DontIgnoreResultOfPutIfAbsent.java` | use | get(int), equals() | public class DontIgnoreResultOfPutIfAbsent |
| 39 | `spotbugs/src/main/java/edu/umd/cs/findbugs/detect/FindBadCast2.java` | use | get(int) | public class FindBadCast2 |
| 40 | `spotbugs/src/main/java/edu/umd/cs/findbugs/detect/FindBugsSummaryStats.java` | use | BitSet(int), set(int), clear(), cardinality() | public class FindBugsSummaryStats |
| 41 | `spotbugs/src/main/java/edu/umd/cs/findbugs/detect/FindDeadLocalStores.java` | use | BitSet(), get(int), set(int) | Find dead stores to local variables |
| 42 | `spotbugs/src/main/java/edu/umd/cs/findbugs/detect/FindJSR166LockMonitorenter.java` | use | get(int) | Find places where ordinary (balanced) synchronization is performed on JSR166 |
| 43 | `spotbugs/src/main/java/edu/umd/cs/findbugs/detect/FindLocalSelfAssignment2.java` | use | BitSet(), get(int), set(int), clear() | public class FindLocalSelfAssignment2 |
| 44 | `spotbugs/src/main/java/edu/umd/cs/findbugs/detect/FindMismatchedWaitOrNotify.java` | use | get(int) | final class FindMismatchedWaitOrNotify |
| 45 | `spotbugs/src/main/java/edu/umd/cs/findbugs/detect/FindNonSerializableStoreIntoSession.java` | use | get(int) | public class FindNonSerializableStoreIntoSession |
| 46 | `spotbugs/src/main/java/edu/umd/cs/findbugs/detect/FindNonSerializableValuePassedToWriteObject.java` | use | get(int) | public class FindNonSerializableValuePassedToWriteObject |
| 47 | `spotbugs/src/main/java/edu/umd/cs/findbugs/detect/FindNullDeref.java` | use | BitSet(), get(int), set(int), clear(int), and(), or(), nextSetBit(), isEmpty(), clone() | A Detector to find instructions where a NullPointerException might be raised |
| 48 | `spotbugs/src/main/java/edu/umd/cs/findbugs/detect/FindOpenStream.java` | use | get(int) | A Detector to look for streams that are opened in a method, do not escape the |
| 49 | `spotbugs/src/main/java/edu/umd/cs/findbugs/detect/FindRefComparison.java` | use | BitSet(), set(int), or(), intersects() | Find suspicious reference comparisons. This includes: |
| 50 | `spotbugs/src/main/java/edu/umd/cs/findbugs/detect/FindSelfComparison.java` | use | get(int) | public class FindSelfComparison |
| 51 | `spotbugs/src/main/java/edu/umd/cs/findbugs/detect/FindSelfComparison2.java` | use | get(int) | @param classContext |
| 52 | `spotbugs/src/main/java/edu/umd/cs/findbugs/detect/FindSleepWithLockHeld.java` | use | get(int) | Find calls to Thread.sleep() made with a lock held |
| 53 | `spotbugs/src/main/java/edu/umd/cs/findbugs/detect/FindUnrelatedTypesInGenericContainer.java` | use | get(int) | @author Nat Ayewah |
| 54 | `spotbugs/src/main/java/edu/umd/cs/findbugs/detect/FindUnreleasedLock.java` | use | get(int) | public class FindUnreleasedLock |
| 55 | `spotbugs/src/main/java/edu/umd/cs/findbugs/detect/FindUseOfNonSerializableValue.java` | use | get(int) | public class FindUseOfNonSerializableValue |
| 56 | `spotbugs/src/main/java/edu/umd/cs/findbugs/detect/FindUselessObjects.java` | use | BitSet(), get(int), set(int), nextSetBit() | @author Tagir Valeev |
| 57 | `spotbugs/src/main/java/edu/umd/cs/findbugs/detect/InfiniteLoop.java` | use | BitSet(), set(int), clear(), nextSetBit() | */ |
| 58 | `spotbugs/src/main/java/edu/umd/cs/findbugs/detect/LazyInit.java` | use | BitSet(), get(int), set(int), clear(), and() | The pattern to look for |
| 59 | `spotbugs/src/main/java/edu/umd/cs/findbugs/detect/LoadOfKnownNullValue.java` | use | BitSet(), get(int), set(int), cardinality() | @param classContext |
| 60 | `spotbugs/src/main/java/edu/umd/cs/findbugs/detect/MethodReturnCheck.java` | use | BitSet(), get(int), set(int) | Look for calls to methods where the return value is erroneously ignored. This |
| 61 | `spotbugs/src/main/java/edu/umd/cs/findbugs/detect/MethodReturnValueStreamFactory.java` | use | BitSet(), get(int), set(int) | StreamFactory for streams that are created as the result of calling a method |
| 62 | `spotbugs/src/main/java/edu/umd/cs/findbugs/detect/NoiseNullDeref.java` | use | BitSet(), get(int), set(int) | A Detector to find instructions where a NullPointerException might be raised |
| 63 | `spotbugs/src/main/java/edu/umd/cs/findbugs/detect/RuntimeExceptionCapture.java` | use | get(int) | RuntimeExceptionCapture |
| 64 | `spotbugs/src/main/java/edu/umd/cs/findbugs/detect/SuspiciousThreadInterrupted.java` | use | BitSet(), get(int), set(int) | looks for calls to Thread.interrupted from a non static context, especially |
| 65 | `spotbugs/src/main/java/edu/umd/cs/findbugs/detect/SwitchFallthrough.java` | use | BitSet(), get(int), set(int), clear(), clear(int), and(), or(), nextSetBit(), cardinality(), clone() | A GOTO might correspond to a <code>break</code> or to a do/while/for loop |
| 66 | `spotbugs/src/main/java/edu/umd/cs/findbugs/props/WarningPropertyUtil.java` | use | BitSet(), get(int), set(int) | Utility methods for creating general warning properties |
| 67 | `spotbugs/src/main/java/edu/umd/cs/findbugs/xml/MetaCharacterMap.java` | use | BitSet(), get(int), set(int) | Map of metacharacters that need to be escaped, and what to replace them with |

### Частотная таблица (use-site файлов)

| Метод | Файлов |
|---|---:|
| `BitSet()` | 42 |
| `BitSet(int)` | 1 |
| `get(int)` | 53 |
| `set(int)` | 39 |
| `set(int,bool)` | 2 |
| `set(from,to)` | 5 |
| `clear()` | 11 |
| `clear(int)` | 8 |
| `and()` | 7 |
| `or()` | 11 |
| `nextSetBit()` | 10 |
| `isEmpty()` | 7 |
| `cardinality()` | 3 |
| `size()` | 1 |
| `intersects()` | 1 |
| `clone()` | 3 |
| `equals()` | 6 |
| `hashCode()` | 1 |
| `toString()` | 1 |

**Итого:** 67 файлов (65 use, 2 impl)

---

## 8. apache/calcite

**Домен:** Database engines
**Исключено (false positive):** 1 файлов

### Каталог файлов

| # | Путь | Cls | Методы / Описание | Контекст |
|---|---|---|---|---|
| 1 | `core/src/main/java/org/apache/calcite/plan/RelOptUtil.java` | use | BitSet(int), size() | <code>RelOptUtil</code> defines static utility methods for use in optimizing |
| 2 | `core/src/main/java/org/apache/calcite/profile/ProfilerImpl.java` | use | BitSet(), clear(int), or(), size() | Implementation of {@link Profiler} that only investigates "interesting" |
| 3 | `core/src/main/java/org/apache/calcite/profile/SimpleProfiler.java` | use | BitSet(), get(int), clear(int), or(), size() | Basic implementation of {@link Profiler} |
| 4 | `core/src/main/java/org/apache/calcite/rel/metadata/RelMdPredicates.java` | use | BitSet(), get(int), set(int), nextSetBit(), cardinality() | Utility to infer Predicates that are applicable above a RelNode |
| 5 | `core/src/main/java/org/apache/calcite/rel/rules/AggregateJoinTransposeRule.java` | use | BitSet(), get(int), set(int) | Planner rule that pushes an |
| 6 | `core/src/main/java/org/apache/calcite/rel/rules/HyperGraph.java` | use | BitSet(), get(int), set(int), set(int,bool), and(), or(), andNot(), stream(), equals() | HyperGraph represents a join graph |
| 7 | `core/src/main/java/org/apache/calcite/rel/rules/LoptMultiJoin.java` | use | get(int) | Utility class that keeps track of the join factors that |
| 8 | `core/src/main/java/org/apache/calcite/rel/rules/LoptOptimizeJoinRule.java` | use | BitSet(int), get(int), set(int), clear(int), and(), or(), cardinality() | Planner rule that implements the heuristic planner for determining optimal |
| 9 | `core/src/main/java/org/apache/calcite/rel/rules/ProjectCorrelateTransposeRule.java` | use | BitSet(), set(int) | Planner rule that pushes a {@link Project} under {@link Correlate} to apply |
| 10 | `core/src/main/java/org/apache/calcite/rel/rules/PushProjector.java` | use | BitSet(int), set(int), set(from,to), or(), nextSetBit(), isEmpty(), cardinality() | PushProjector is a utility class used to perform operations used in push |
| 11 | `core/src/main/java/org/apache/calcite/rex/RexSimplify.java` | use | BitSet(), get(int), set(int) | Context required to simplify a row-expression |
| 12 | `core/src/main/java/org/apache/calcite/sql/validate/SqlValidatorImpl.java` | use | BitSet(), get(int), set(int), isEmpty() | Default implementation of {@link SqlValidator} |
| 13 | `core/src/main/java/org/apache/calcite/sql2rel/SqlToRelConverter.java` | use | BitSet(), set(int), cardinality() | Converts a SQL parse tree (consisting of |
| 14 | `core/src/main/java/org/apache/calcite/tools/RelBuilder.java` | use | BitSet(), get(int), set(int) | Builder for relational expressions |
| 15 | `core/src/main/java/org/apache/calcite/util/ArrowSet.java` | use | BitSet(), get(int), set(int), stream() | Represents a set of functional dependencies. Each functional dependency is an... |
| 16 | `core/src/main/java/org/apache/calcite/util/BitSets.java` | impl | Утилиты: contains (superset check), toIter, forEach по set-битам; отсутствуют в j.u.BitSet | Utility functions for {@link BitSet} |
| 17 | `core/src/main/java/org/apache/calcite/util/ImmutableBitSet.java` | impl | Immutable, Comparable, Iterable<Integer> BitSet с value-семантикой; компенсирует мутабельность и отсутствие Iterable/Comparable | An immutable list of bits |
| 18 | `core/src/main/java/org/apache/calcite/util/mapping/Mappings.java` | use | BitSet(), set(int), equals() | Utility functions related to mappings |
| 19 | `core/src/test/java/org/apache/calcite/util/BitSetsTest.java` | test |  | Unit test for {@link org.apache.calcite.util.BitSets} |
| 20 | `core/src/test/java/org/apache/calcite/util/UtilTest.java` | test |  | Unit test for {@link Util} and other classes in this package |
| 21 | `testkit/src/main/java/org/apache/calcite/test/schemata/catchall/CatchallSchema.java` | test |  | Object whose fields are relations. Called "catch-all" because it's OK |

### Частотная таблица (use-site файлов)

| Метод | Файлов |
|---|---:|
| `BitSet()` | 12 |
| `BitSet(int)` | 3 |
| `get(int)` | 10 |
| `set(int)` | 12 |
| `set(int,bool)` | 1 |
| `set(from,to)` | 1 |
| `clear(int)` | 3 |
| `and()` | 2 |
| `or()` | 5 |
| `andNot()` | 1 |
| `nextSetBit()` | 2 |
| `isEmpty()` | 2 |
| `cardinality()` | 4 |
| `size()` | 3 |
| `stream()` | 2 |
| `equals()` | 2 |

**Итого:** 21 файлов (16 use, 2 impl, 3 test)

---

## 9. h2database/h2database

**Домен:** Database engines
**Исключено (false positive):** 1 файлов

### Каталог файлов

| # | Путь | Cls | Методы / Описание | Контекст |
|---|---|---|---|---|
| 1 | `h2/src/main/org/h2/command/Parser.java` | use | (тип в сигнатуре) | The parser is used to convert a SQL statement string to an command object |
| 2 | `h2/src/main/org/h2/command/ParserBase.java` | use | BitSet(), get(int), set(int), or(), nextSetBit(), isEmpty(), cardinality(), length() | The base class for the parser |
| 3 | `h2/src/main/org/h2/command/Tokenizer.java` | use | get(int), set(int) | Tokenizer |
| 4 | `h2/src/main/org/h2/command/query/Optimizer.java` | use | BitSet(), get(int), set(int) | The optimizer is responsible to find the best execution plan |
| 5 | `h2/src/main/org/h2/command/query/Select.java` | use | BitSet(), nextSetBit(), cardinality() | This class represents a simple SELECT statement |
| 6 | `h2/src/main/org/h2/engine/Database.java` | use | BitSet(), get(int), set(int), andNot(), nextClearBit() | There is one database object per open database |
| 7 | `h2/src/main/org/h2/engine/SessionLocal.java` | use | BitSet(), set(int) | A session represents an embedded database connection. When using the server |
| 8 | `h2/src/main/org/h2/jdbc/JdbcCallableStatement.java` | use | BitSet(), get(int), set(int) | Represents a callable statement |
| 9 | `h2/src/main/org/h2/mvstore/Chunk.java` | use | BitSet(), get(int), set(int), isEmpty(), cardinality(), toByteArray(), valueOf(...) | A chunk of data, containing one or multiple pages |
| 10 | `h2/src/main/org/h2/mvstore/FileStore.java` | use | BitSet() | Class FileStore is a base class to allow for different store implementations |
| 11 | `h2/src/main/org/h2/mvstore/FreeSpaceBitSet.java` | impl | Block-size-aware addressing + contiguous-range allocation; j.u.BitSet не знает о sized blocks и lifecycle mark/free | A free space bit set |
| 12 | `h2/src/main/org/h2/mvstore/db/MVSortedTempResult.java` | use | BitSet(), get(int), set(int), nextClearBit() | Sorted temporary result |
| 13 | `h2/src/main/org/h2/mvstore/db/Store.java` | use | get(int) | A store with open tables |
| 14 | `h2/src/main/org/h2/mvstore/tx/CommitDecisionMaker.java` | use | BitSet(int), get(int), set(int) | Class CommitDecisionMaker makes a decision during post-commit processing |
| 15 | `h2/src/main/org/h2/mvstore/tx/Transaction.java` | use | BitSet(), get(int), set(int) | A transaction |
| 16 | `h2/src/main/org/h2/table/InformationSchemaTable.java` | use | (тип в сигнатуре) | This class is responsible to build the INFORMATION_SCHEMA tables |
| 17 | `h2/src/main/org/h2/table/InformationSchemaTableLegacy.java` | use | (тип в сигнатуре) | This class is responsible to build the legacy variant of INFORMATION_SCHEMA |
| 18 | `h2/src/test/org/h2/test/db/TestBigResult.java` | test |  | Test for big result sets |
| 19 | `h2/src/test/org/h2/test/store/CalculateHashConstant.java` | test |  | Calculate the constant for the secondary / supplemental hash function, so |
| 20 | `h2/src/test/org/h2/test/store/CalculateHashConstantLong.java` | test |  | Calculate the constant for the secondary hash function, so that the hash |
| 21 | `h2/src/test/org/h2/test/unit/TestIntPerfectHash.java` | test |  | Tests the perfect hash tool |
| 22 | `h2/src/test/org/h2/test/unit/TestMVTempResult.java` | test |  | Tests that MVTempResult implementations do not produce OOME |
| 23 | `h2/src/test/org/h2/test/unit/TestPerfectHash.java` | test |  | Tests the perfect hash tool |

### Частотная таблица (use-site файлов)

| Метод | Файлов |
|---|---:|
| `BitSet()` | 10 |
| `BitSet(int)` | 1 |
| `get(int)` | 10 |
| `set(int)` | 10 |
| `or()` | 1 |
| `andNot()` | 1 |
| `nextSetBit()` | 2 |
| `nextClearBit()` | 2 |
| `isEmpty()` | 2 |
| `cardinality()` | 3 |
| `length()` | 1 |
| `toByteArray()` | 1 |
| `valueOf(...)` | 1 |

**Итого:** 23 файлов (16 use, 1 impl, 6 test)

---

## 10. checkstyle/checkstyle

**Домен:** Static analysis / code quality
**Исключено (false positive):** 20 файлов

### Каталог файлов

| # | Путь | Cls | Методы / Описание | Контекст |
|---|---|---|---|---|
| 1 | `src/it/resources/com/google/checkstyle/test/chapter3filestructure/rule331nowildcard/InputNoWildcardImports.java` | test |  | InputNoWildcardImports.java |
| 2 | `src/it/resources/com/google/checkstyle/test/chapter3filestructure/rule333orderingandspacing/InputFormattedOrderingAndSpacingValid.java` | test |  | public class InputFormattedOrderingAndSpacingValid |
| 3 | `src/it/resources/com/google/checkstyle/test/chapter3filestructure/rule333orderingandspacing/InputFormattedOrderingAndSpacingValid2.java` | test |  | public class InputFormattedOrderingAndSpacingValid2 |
| 4 | `src/it/resources/com/google/checkstyle/test/chapter3filestructure/rule333orderingandspacing/InputOrderingAndSpacingValid.java` | test |  | public class InputOrderingAndSpacingValid |
| 5 | `src/it/resources/com/google/checkstyle/test/chapter3filestructure/rule333orderingandspacing/InputOrderingAndSpacingValid2.java` | test |  | public class InputOrderingAndSpacingValid2 |
| 6 | `src/main/java/com/puppycrawl/tools/checkstyle/DetailAstImpl.java` | use | BitSet(), set(int), or() | The implementation of {@link DetailAST}. This should only be directly used to |
| 7 | `src/main/java/com/puppycrawl/tools/checkstyle/checks/FinalParametersCheck.java` | use | get(int) | <div> |
| 8 | `src/main/java/com/puppycrawl/tools/checkstyle/checks/annotation/MissingDeprecatedCheck.java` | use | get(int) | <div> |
| 9 | `src/main/java/com/puppycrawl/tools/checkstyle/checks/coding/AvoidDoubleBraceInitializationCheck.java` | use | get(int) | <div> |
| 10 | `src/main/java/com/puppycrawl/tools/checkstyle/checks/coding/FinalLocalVariableCheck.java` | use | get(int) | <div> |
| 11 | `src/main/java/com/puppycrawl/tools/checkstyle/checks/coding/IllegalTypeCheck.java` | use | BitSet(), get(int), isEmpty() | <div> |
| 12 | `src/main/java/com/puppycrawl/tools/checkstyle/checks/coding/InnerAssignmentCheck.java` | use | get(int) | <div> |
| 13 | `src/main/java/com/puppycrawl/tools/checkstyle/checks/coding/MagicNumberCheck.java` | use | get(int) | <div> |
| 14 | `src/main/java/com/puppycrawl/tools/checkstyle/checks/coding/ModifiedControlVariableCheck.java` | use | get(int) | <div> |
| 15 | `src/main/java/com/puppycrawl/tools/checkstyle/checks/coding/MultipleStringLiteralsCheck.java` | use | BitSet(), get(int), set(int), clear() | <div> |
| 16 | `src/main/java/com/puppycrawl/tools/checkstyle/checks/coding/RequireThisCheck.java` | use | get(int) | <div> |
| 17 | `src/main/java/com/puppycrawl/tools/checkstyle/checks/design/InnerTypeLastCheck.java` | use | get(int) | <div> |
| 18 | `src/main/java/com/puppycrawl/tools/checkstyle/checks/header/HeaderCheck.java` | use | BitSet(), get(int) | <div> |
| 19 | `src/main/java/com/puppycrawl/tools/checkstyle/checks/header/RegexpHeaderCheck.java` | use | BitSet(), get(int), cardinality() | <div> |
| 20 | `src/main/java/com/puppycrawl/tools/checkstyle/checks/indentation/IndentLevel.java` | use | BitSet(), get(int), set(int), or(), nextSetBit(), cardinality(), length() | Encapsulates representation of notion of expected indentation levels |
| 21 | `src/main/java/com/puppycrawl/tools/checkstyle/checks/indentation/SlistHandler.java` | use | get(int) | Handler for a list of statements |
| 22 | `src/main/java/com/puppycrawl/tools/checkstyle/checks/javadoc/AtclauseOrderCheck.java` | use | get(int) | <div> |
| 23 | `src/main/java/com/puppycrawl/tools/checkstyle/checks/javadoc/JavadocTagInfo.java` | use | get(int) | This enum defines the various Javadoc tags and their properties |
| 24 | `src/main/java/com/puppycrawl/tools/checkstyle/checks/sizes/MethodLengthCheck.java` | use | BitSet(), set(int), set(from,to), cardinality() | <div> |
| 25 | `src/main/java/com/puppycrawl/tools/checkstyle/checks/whitespace/ParenPadCheck.java` | use | get(int) | <div> |
| 26 | `src/main/java/com/puppycrawl/tools/checkstyle/site/SiteUtil.java` | use | stream() | Utility class for site generation |
| 27 | `src/main/java/com/puppycrawl/tools/checkstyle/utils/CommonUtil.java` | use | BitSet() | Contains utility methods |
| 28 | `src/main/java/com/puppycrawl/tools/checkstyle/utils/TokenUtil.java` | use | (тип в сигнатуре) | Contains utility methods for tokens |
| 29 | `src/main/java/com/puppycrawl/tools/checkstyle/utils/XpathUtil.java` | use | get(int) | Contains utility methods for xpath |
| 30 | `src/test/java/com/puppycrawl/tools/checkstyle/DetailAstImplTest.java` | test |  | TestCase to check DetailAST |
| 31 | `src/test/java/com/puppycrawl/tools/checkstyle/bdd/InlineConfigParser.java` | test |  | Pattern for lines under |
| 32 | `src/test/java/com/puppycrawl/tools/checkstyle/internal/AllChecksTest.java` | test |  | Checks that an array is a subset of other array |
| 33 | `src/test/java/com/puppycrawl/tools/checkstyle/internal/XdocsPagesTest.java` | test |  | Generates xdocs pages from templates and performs validations |
| 34 | `src/test/resources/com/puppycrawl/tools/checkstyle/checks/coding/requirethis/InputRequireThisValidateOnlyOverlappingFalse.java` | test |  | public class InputRequireThisValidateOnlyOverlappingFalse |
| 35 | `src/test/resources/com/puppycrawl/tools/checkstyle/checks/coding/requirethis/InputRequireThisValidateOnlyOverlappingTrue.java` | test |  | public class InputRequireThisValidateOnlyOverlappingTrue |

### Частотная таблица (use-site файлов)

| Метод | Файлов |
|---|---:|
| `BitSet()` | 8 |
| `get(int)` | 19 |
| `set(int)` | 4 |
| `set(from,to)` | 1 |
| `clear()` | 1 |
| `or()` | 2 |
| `nextSetBit()` | 1 |
| `isEmpty()` | 1 |
| `cardinality()` | 3 |
| `length()` | 1 |
| `stream()` | 1 |

**Итого:** 35 файлов (24 use, 11 test)

---

## 11. pmd/pmd

**Домен:** Static analysis / code quality

### Каталог файлов

| # | Путь | Cls | Методы / Описание | Контекст |
|---|---|---|---|---|
| 1 | `pmd-core/src/main/java/net/sourceforge/pmd/cpd/MatchCollector.java` | use | BitSet(), get(int), set(int) | MatchCollector.java |
| 2 | `pmd-java/src/main/java/net/sourceforge/pmd/lang/java/ast/OverrideResolutionPass.java` | use | BitSet(int), get(int), set(int) | Populates method declarations with the method they override |
| 3 | `pmd-java/src/main/java/net/sourceforge/pmd/lang/java/symbols/table/internal/JavaResolvers.java` | use | BitSet(int), get(int), set(int), nextSetBit(), isEmpty(), size() | Returns true if the given element can be imported in the current file |

### Частотная таблица (use-site файлов)

| Метод | Файлов |
|---|---:|
| `BitSet()` | 1 |
| `BitSet(int)` | 2 |
| `get(int)` | 3 |
| `set(int)` | 3 |
| `nextSetBit()` | 1 |
| `isEmpty()` | 1 |
| `size()` | 1 |

**Итого:** 3 файлов (3 use)

---

## 12. apache/flink

**Домен:** Data processing / pipeline

### Каталог файлов

| # | Путь | Cls | Методы / Описание | Контекст |
|---|---|---|---|---|
| 1 | `flink-connectors/flink-connector-base/src/test/java/org/apache/flink/connector/base/source/reader/synchronization/FutureCompletingBlockingQueueTest.java` | test |  | This test is to guard that our reflection is not broken and we do not lose th... |
| 2 | `flink-runtime/src/main/java/org/apache/flink/runtime/checkpoint/DefaultCheckpointPlanCalculator.java` | use | BitSet(int), get(int), set(int), cardinality(), size() | Default implementation for {@link CheckpointPlanCalculator}. If all tasks are... |
| 3 | `flink-runtime/src/main/java/org/apache/flink/runtime/checkpoint/RescaleMappings.java` | use | BitSet(int), get(int), set(int) | Contains the fine-grain channel mappings that occur when a connected operator... |
| 4 | `flink-runtime/src/main/java/org/apache/flink/runtime/checkpoint/RoundRobinOperatorStateRepartitioner.java` | use | BitSet(int), set(int), cardinality() | Current default implementation of {@link OperatorStateRepartitioner} that red... |
| 5 | `flink-runtime/src/main/java/org/apache/flink/runtime/io/network/partition/consumer/SingleInputGate.java` | use | BitSet(int), get(int), set(int), clear(int), nextClearBit(), cardinality() | An input gate consumes one or more partitions of a single produced intermedia... |
| 6 | `flink-runtime/src/main/java/org/apache/flink/runtime/scheduler/VertexEndOfDataListener.java` | use | BitSet(), get(int), set(int), cardinality() | Records the end of data event of each task, and allows for checking whether a... |
| 7 | `flink-runtime/src/main/java/org/apache/flink/streaming/runtime/watermark/AlignedWatermarkCombiner.java` | use | BitSet(int), set(int), clear(), cardinality() | A {@link WatermarkCombiner} is design to align {@link Watermark}s. It will co... |
| 8 | `flink-runtime/src/main/java/org/apache/flink/streaming/runtime/watermark/BoolWatermarkCombiner.java` | use | BitSet(int), set(int), set(from,to), clear(from,to), cardinality() | A {@link WatermarkCombiner} for unaligned {@link BoolWatermark}s |
| 9 | `flink-runtime/src/main/java/org/apache/flink/streaming/runtime/watermark/LongWatermarkCombiner.java` | use | BitSet(int), set(int), cardinality() | A {@link WatermarkCombiner} for unaligned {@link LongWatermark}s |
| 10 | `flink-runtime/src/main/java/org/apache/flink/streaming/runtime/watermark/extension/eventtime/EventTimeWatermarkHandler.java` | use | BitSet(int), set(int), cardinality() | This class is used to handle {@link EventTimeExtension} related watermarks in... |
| 11 | `flink-runtime/src/test/java/org/apache/flink/runtime/jobmanager/SlotCountExceedingParallelismTest.java` | test |  | Tests that Flink can execute jobs with a higher parallelism than available nu... |
| 12 | `flink-runtime/src/test/java/org/apache/flink/runtime/operators/hash/CompactingHashTableTest.java` | test |  | This has to be duplicated in InPlaceMutableHashTableTest and CompactingHashTa... |
| 13 | `flink-runtime/src/test/java/org/apache/flink/runtime/operators/hash/InPlaceMutableHashTableTest.java` | test |  | This has to be duplicated in InPlaceMutableHashTableTest and CompactingHashTa... |
| 14 | `flink-streaming-java/src/main/java/org/apache/flink/streaming/util/serialize/FlinkChillPackageRegistrar.java` | use | (тип в сигнатуре) | Registers all chill serializers used for Java types |
| 15 | `flink-streaming-java/src/test/java/org/apache/flink/streaming/runtime/operators/windowing/KeyMapPutTest.java` | test |  | KeyMapPutTest.java |
| 16 | `flink-table/flink-table-common/src/main/java/org/apache/flink/table/utils/PartitionPathUtils.java` | use | BitSet(int), get(int), set(int), size() | Make partition path from partition spec |
| 17 | `flink-table/flink-table-planner/src/main/java/org/apache/calcite/rel/metadata/RelMdPredicates.java` | use | get(int), set(int), nextSetBit(), cardinality() | Utility to infer Predicates that are applicable above a RelNode |
| 18 | `flink-table/flink-table-planner/src/main/java/org/apache/calcite/sql2rel/SqlToRelConverter.java` | use | BitSet(), set(int) | Converts a SQL parse tree (consisting of {@link org.apache.calcite.sql.SqlNod... |
| 19 | `flink-table/flink-table-planner/src/main/java/org/apache/flink/table/planner/plan/rules/logical/FlinkAggregateJoinTransposeRule.java` | use | BitSet(), get(int), set(int) | This rule is copied from Calcite's {@link |
| 20 | `flink-table/flink-table-runtime/src/main/java/org/apache/flink/table/data/UpdatableRowData.java` | use | BitSet(int), get(int), set(int) | An implementation of {@link RowData} which is backed by a {@link RowData} and... |
| 21 | `flink-table/flink-table-runtime/src/main/java/org/apache/flink/table/runtime/operators/join/SortMergeJoinFunction.java` | use | BitSet(), get(int), set(int), clear() | public class SortMergeJoinFunction |
| 22 | `flink-table/flink-table-runtime/src/main/java/org/apache/flink/table/runtime/operators/sink/constraint/BinaryLengthConstraint.java` | use | get(int) | final class BinaryLengthConstraint |
| 23 | `flink-table/flink-table-runtime/src/main/java/org/apache/flink/table/runtime/operators/sink/constraint/CharLengthConstraint.java` | use | get(int) | final class CharLengthConstraint |
| 24 | `flink-table/flink-table-runtime/src/main/java/org/apache/flink/table/runtime/operators/sink/constraint/ConstraintEnforcerExecutor.java` | use | BitSet(int), set(int), size() | Logic extracted from {@link ConstraintEnforcer} in order to use it outside of... |
| 25 | `flink-tests/src/test/java/org/apache/flink/runtime/operators/lifecycle/validation/DrainingValidator.java` | test |  | For each input, checks that the {@link Watermark#MAX_WATERMARK} was received ... |
| 26 | `flink-tests/src/test/java/org/apache/flink/test/checkpointing/UnalignedCheckpointRescaleITCase.java` | test |  | Creates a FailingMapper that only fails during snapshot operations |
| 27 | `flink-tests/src/test/java/org/apache/flink/test/state/operator/restore/StreamOperatorSnapshotRestoreTest.java` | test |  | Test restoring an operator from a snapshot (local recovery activated, JM snap... |
| 28 | `flink-tests/src/test/java/org/apache/flink/test/windowing/sessionwindows/SessionWindowITCase.java` | test |  | public class SessionWindowITCase |

### Частотная таблица (use-site файлов)

| Метод | Файлов |
|---|---:|
| `BitSet()` | 4 |
| `BitSet(int)` | 11 |
| `get(int)` | 11 |
| `set(int)` | 16 |
| `set(from,to)` | 1 |
| `clear()` | 2 |
| `clear(int)` | 1 |
| `clear(from,to)` | 1 |
| `nextSetBit()` | 1 |
| `nextClearBit()` | 1 |
| `cardinality()` | 9 |
| `size()` | 3 |

**Итого:** 28 файлов (19 use, 9 test)

---

## 13. netty/netty

**Домен:** Networking

### Каталог файлов

| # | Путь | Cls | Методы / Описание | Контекст |
|---|---|---|---|---|
| 1 | `codec-base/src/main/java/io/netty/handler/codec/DateFormatter.java` | use | BitSet(), get(int), set(int) | A formatter for HTTP header dates, such as "Expires" and "Date" headers, or "... |
| 2 | `codec-http/src/main/java/io/netty/handler/codec/http/CookieUtil.java` | use | BitSet(int), get(int), set(int), set(int,bool), set(from,to,bool), or() | @deprecated Duplicate of package private ${@link io.netty.handler.codec.http.... |
| 3 | `codec-http/src/main/java/io/netty/handler/codec/http/HttpChunkLineValidatingByteProcessor.java` | impl | Match extends BitSet: fluent builder (chars/range) + state transition; нет bulk-set-from-string и range-set convenience API | Validates the chunk start line. That is, the chunk size and chunk extensions,... |
| 4 | `codec-http/src/main/java/io/netty/handler/codec/http/cookie/CookieUtil.java` | use | BitSet(), get(int), set(int), set(int,bool) | @param buf a buffer where some cookies were maybe encoded |
| 5 | `common/src/main/java/io/netty/util/internal/InternalThreadLocalMap.java` | use | BitSet(), get(int), set(int) | The internal data structure that stores the thread-local variables for Netty ... |

### Частотная таблица (use-site файлов)

| Метод | Файлов |
|---|---:|
| `BitSet()` | 3 |
| `BitSet(int)` | 1 |
| `get(int)` | 4 |
| `set(int)` | 4 |
| `set(int,bool)` | 2 |
| `set(from,to,bool)` | 1 |
| `or()` | 1 |

**Итого:** 5 файлов (4 use, 1 impl)

---

## 14. eclipse-collections/eclipse-collections

**Домен:** Collections / data structures

### Каталог файлов

| # | Путь | Cls | Методы / Описание | Контекст |
|---|---|---|---|---|
| 1 | `eclipse-collections/src/main/java/org/eclipse/collections/impl/list/immutable/primitive/ImmutableBooleanArrayList.java` | use | BitSet(int), get(int), set(int), set(from,to), size(), clone() | ImmutableBooleanArrayList is the non-modifiable equivalent of {@link BooleanA... |
| 2 | `eclipse-collections/src/main/java/org/eclipse/collections/impl/list/mutable/primitive/BooleanArrayList.java` | use | BitSet(), BitSet(int), get(int), set(int), set(from,to), set(from,to,bool), clear(), clear(int), clear(from,to), size() | BooleanArrayList is similar to {@link FastList}, and is memory-optimized for ... |
| 3 | `eclipse-collections/src/main/java/org/eclipse/collections/impl/map/mutable/primitive/ObjectBooleanHashMap.java` | use | BitSet(int), get(int), set(int,bool), set(from,to), clear(), size(), hashCode() | @since 3.0 |
| 4 | `eclipse-collections/src/main/java/org/eclipse/collections/impl/map/mutable/primitive/ObjectBooleanHashMapWithHashingStrategy.java` | use | BitSet(int), get(int), set(int,bool), set(from,to), clear(), size() | @since 7.0 |
| 5 | `unit-tests/src/test/java/org/eclipse/collections/impl/list/mutable/primitive/BooleanArrayListTest.java` | test |  | JUnit test for {@link BooleanArrayList} |
| 6 | `unit-tests/src/test/java/org/eclipse/collections/impl/map/mutable/primitive/ObjectBooleanHashMapTestCase.java` | test |  | abstract class ObjectBooleanHashMapTestCase |

### Частотная таблица (use-site файлов)

| Метод | Файлов |
|---|---:|
| `BitSet()` | 1 |
| `BitSet(int)` | 4 |
| `get(int)` | 4 |
| `set(int)` | 2 |
| `set(int,bool)` | 2 |
| `set(from,to)` | 4 |
| `set(from,to,bool)` | 1 |
| `clear()` | 3 |
| `clear(int)` | 1 |
| `clear(from,to)` | 1 |
| `size()` | 4 |
| `clone()` | 1 |
| `hashCode()` | 1 |

**Итого:** 6 файлов (4 use, 2 test)

---

## 15. apache/druid

**Домен:** Database engines

### Каталог файлов

| # | Путь | Cls | Методы / Описание | Контекст |
|---|---|---|---|---|
| 1 | `benchmarks/src/test/java/org/apache/druid/benchmark/ExpressionSelectorBenchmark.java` | test |  | public class ExpressionSelectorBenchmark |
| 2 | `benchmarks/src/test/java/org/apache/druid/benchmark/NullHandlingBitmapGetVsIteratorBenchmark.java` | test |  | public class NullHandlingBitmapGetVsIteratorBenchmark |
| 3 | `benchmarks/src/test/java/org/apache/druid/benchmark/compression/CompressedColumnarIntsBenchmark.java` | test |  | public class CompressedColumnarIntsBenchmark |
| 4 | `benchmarks/src/test/java/org/apache/druid/benchmark/compression/CompressedVSizeColumnarMultiIntsBenchmark.java` | test |  | public class CompressedVSizeColumnarMultiIntsBenchmark |
| 5 | `extensions-contrib/spectator-histogram/src/main/java/org/apache/druid/spectator/histogram/NullableOffsetsHeader.java` | use | BitSet(), set(int), cardinality(), length(), toByteArray() | A header for storing offsets for columns with nullable values |
| 6 | `processing/src/main/java/org/apache/druid/collections/bitmap/BitSetBitmapFactory.java` | impl | Адаптер j.u.BitSet -> BitmapFactory интерфейс; нет абстрактного factory/strategy контракта | BitSetBitmapFactory implements BitmapFactory as a wrapper for java.util.BitSet |
| 7 | `processing/src/main/java/org/apache/druid/collections/bitmap/WrappedBitSetBitmap.java` | impl | Адаптер j.u.BitSet -> MutableBitmap интерфейс; нет unified bitmap contract для взаимозаменяемости с Roaring/Concise | WrappedBitSetBitmap implements MutableBitmap for java.util.BitSet |
| 8 | `processing/src/main/java/org/apache/druid/collections/bitmap/WrappedImmutableBitSetBitmap.java` | impl | Immutable адаптер с IntIterator и toBytes(); нет immutable контракта и iterator protocol в j.u.BitSet | WrappedImmutableBitSetBitmap implements ImmutableBitmap for java.util.BitSet |
| 9 | `processing/src/main/java/org/apache/druid/query/dimension/ForwardingFilteredDimensionSelector.java` | use | (тип в сигнатуре) | @param selector must return true from {@link DimensionSelector#nameLookupPoss... |
| 10 | `processing/src/main/java/org/apache/druid/query/filter/vector/MultiValueStringVectorValueMatcher.java` | use | BitSet(int), get(int), set(int) | public class MultiValueStringVectorValueMatcher |
| 11 | `processing/src/main/java/org/apache/druid/query/filter/vector/SingleValueStringVectorValueMatcher.java` | use | BitSet(int), get(int), set(int) | public class SingleValueStringVectorValueMatcher |
| 12 | `processing/src/main/java/org/apache/druid/query/groupby/GroupByQueryQueryToolChest.java` | use | BitSet(), get(int), set(int), nextSetBit(), isEmpty() | Toolchest for GroupBy queries |
| 13 | `processing/src/main/java/org/apache/druid/query/groupby/epinephelinae/RowBasedGrouperHelper.java` | use | BitSet(int), get(int), set(int), size() | This class contains shared code between {@link GroupByMergingQueryRunner} and... |
| 14 | `processing/src/main/java/org/apache/druid/query/rowsandcols/LazilyDecoratedRowsAndColumns.java` | use | BitSet(int), get(int), set(int), set(int,bool) | public class LazilyDecoratedRowsAndColumns |
| 15 | `processing/src/main/java/org/apache/druid/segment/DimensionSelectorUtils.java` | use | BitSet(int), get(int), set(int) | Generic implementation of {@link DimensionSelector#makeValueMatcher(String)},... |
| 16 | `processing/src/main/java/org/apache/druid/segment/RowCombiningTimeAndDimsIterator.java` | use | BitSet(), set(int), clear(), nextSetBit() | RowCombiningTimeAndDimsIterator takes some {@link RowIterator}s, assuming tha... |
| 17 | `processing/src/main/java/org/apache/druid/segment/StringDimensionIndexer.java` | use | BitSet(int), get(int), set(int) | Truncates the value to the first {@link #maxStringLength} characters if confi... |
| 18 | `processing/src/main/java/org/apache/druid/segment/column/StringUtf8DictionaryEncodedColumn.java` | use | BitSet(int), get(int), set(int) | {@link DictionaryEncodedColumn<String>} for a column which has a {@link ByteB... |
| 19 | `processing/src/main/java/org/apache/druid/segment/nested/NestedFieldDictionaryEncodedColumn.java` | use | BitSet(int), get(int), set(int) | Lookup value from appropriate scalar value dictionary, coercing the value to ... |
| 20 | `processing/src/main/java/org/apache/druid/segment/nested/VariantColumn.java` | use | BitSet(int), get(int), set(int) | {@link NestedCommonFormatColumn} for single type array columns, and mixed typ... |
| 21 | `processing/src/test/java/org/apache/druid/collections/IntSetTestUtility.java` | test |  | */ |
| 22 | `processing/src/test/java/org/apache/druid/collections/bitmap/BatchIteratorAdapterTest.java` | test |  | public class BatchIteratorAdapterTest |
| 23 | `processing/src/test/java/org/apache/druid/collections/bitmap/BitmapOperationAgainstConsecutiveRunsTest.java` | test |  | public class BitmapOperationAgainstConsecutiveRunsTest |
| 24 | `processing/src/test/java/org/apache/druid/collections/bitmap/BitmapOperationAgainstUniformDistributionTest.java` | test |  | public class BitmapOperationAgainstUniformDistributionTest |
| 25 | `processing/src/test/java/org/apache/druid/collections/bitmap/WrappedBitSetBitmapBitSetTest.java` | test |  | */ |
| 26 | `processing/src/test/java/org/apache/druid/java/util/emitter/core/HttpPostEmitterStressTest.java` | test |  | public class HttpPostEmitterStressTest |
| 27 | `processing/src/test/java/org/apache/druid/segment/IndexIOTest.java` | test |  | This is mostly a test of the validator |
| 28 | `processing/src/test/java/org/apache/druid/segment/data/TestColumnCompression.java` | test |  | public class TestColumnCompression |

### Частотная таблица (use-site файлов)

| Метод | Файлов |
|---|---:|
| `BitSet()` | 3 |
| `BitSet(int)` | 9 |
| `get(int)` | 10 |
| `set(int)` | 12 |
| `set(int,bool)` | 1 |
| `clear()` | 1 |
| `nextSetBit()` | 2 |
| `isEmpty()` | 1 |
| `cardinality()` | 1 |
| `size()` | 1 |
| `length()` | 1 |
| `toByteArray()` | 1 |

**Итого:** 28 файлов (13 use, 3 impl, 12 test)

---

## 16. apache/hive

**Домен:** Data processing / pipeline

### Каталог файлов

| # | Путь | Cls | Методы / Описание | Контекст |
|---|---|---|---|---|
| 1 | `common/src/java/org/apache/hadoop/hive/common/FileUtils.java` | use | BitSet(int), get(int), set(int), size() | Collection of file manipulation utilities common across Hive |
| 2 | `iceberg/iceberg-handler/src/main/java/org/apache/iceberg/mr/hive/writer/ParquetVariantRecordWriter.java` | use | BitSet(int), set(int), nextClearBit(), size() | Writer-side helper that buffers a small sample of records to initialize Parqu... |
| 3 | `ql/src/java/org/apache/hadoop/hive/ql/ddl/table/partition/add/AlterTableAddPartitionOperation.java` | use | BitSet() | Operation process of adding a partition to a table |
| 4 | `ql/src/java/org/apache/hadoop/hive/ql/exec/ColumnStatsUpdateTask.java` | use | BitSet() | ColumnStatsUpdateTask implementation. For example, ALTER TABLE src_stat |
| 5 | `ql/src/java/org/apache/hadoop/hive/ql/exec/FileSinkOperator.java` | use | BitSet(), get(int), set(int) | File Sink operator implementation |
| 6 | `ql/src/java/org/apache/hadoop/hive/ql/exec/tez/HiveSplitGenerator.java` | use | get(int), toString() | This class is used to generate splits inside the AM on the cluster. It |
| 7 | `ql/src/java/org/apache/hadoop/hive/ql/io/orc/VectorizedOrcAcidRowBatchReader.java` | use | BitSet(int), set(int), set(from,to,bool), clear(int), clear(from,to), nextSetBit(), previousSetBit(), cardinality(), size(), clone() | A fast vectorized batch reader class for ACID. Insert events are read directly |
| 8 | `ql/src/java/org/apache/hadoop/hive/ql/metadata/Hive.java` | use | BitSet() | This class has functions that implement meta data/DDL operations using calls |
| 9 | `ql/src/java/org/apache/hadoop/hive/ql/optimizer/ConstantPropagateProcFactory.java` | use | BitSet(), get(int), set(int), nextSetBit() | Factory for generating the different node processors used by ConstantPropagate |
| 10 | `ql/src/java/org/apache/hadoop/hive/ql/optimizer/FixedBucketPruningOptimizer.java` | use | BitSet(int), set(int), clear(), cardinality() | Fixed bucket pruning optimizer goes through all the table scans and annotates... |
| 11 | `ql/src/java/org/apache/hadoop/hive/ql/optimizer/GroupingSetOptimizer.java` | use | nextSetBit(), valueOf(...) | public class GroupingSetOptimizer |
| 12 | `ql/src/java/org/apache/hadoop/hive/ql/optimizer/SharedWorkOptimizer.java` | use | BitSet(), get(int), set(int), cardinality() | Shared computation optimizer |
| 13 | `ql/src/java/org/apache/hadoop/hive/ql/optimizer/calcite/rules/HiveAggregateJoinTransposeRule.java` | use | BitSet(), get(int), get(from,to), set(int) | Planner rule that pushes an |
| 14 | `ql/src/java/org/apache/hadoop/hive/ql/optimizer/calcite/rules/HiveCardinalityPreservingJoinOptimization.java` | use | BitSet(), BitSet(int), get(int), set(int), stream() | Optimization to reduce the amount of broadcasted/shuffled data throughout the... |
| 15 | `ql/src/java/org/apache/hadoop/hive/ql/optimizer/calcite/rules/HiveLoptMultiJoin.java` | use | get(int) | Join filters associated with the MultiJoin, decomposed into a list |
| 16 | `ql/src/java/org/apache/hadoop/hive/ql/optimizer/calcite/rules/HiveLoptOptimizeJoinRule.java` | use | BitSet(int), get(int), set(int), clear(int), and(), or(), cardinality() | Locates all null generating factors whose outer join can be removed. The |
| 17 | `ql/src/java/org/apache/hadoop/hive/ql/optimizer/calcite/stats/EstimateUniqueKeys.java` | use | BitSet(), get(int), set(int) | EstimateUniqueKeys provides an ability to estimate unique keys based on stati... |
| 18 | `ql/src/java/org/apache/hadoop/hive/ql/optimizer/calcite/stats/HiveRelMdPredicates.java` | use | get(int), set(int), nextSetBit(), cardinality(), toString() | Infers predicates for a project |
| 19 | `ql/src/java/org/apache/hadoop/hive/ql/parse/CalcitePlanner.java` | use | BitSet(), set(int), flip(from,to), size() | {@link org.antlr.runtime.TokenRewriteStream} offers the opportunity of multip... |
| 20 | `ql/src/java/org/apache/hadoop/hive/ql/plan/MapWork.java` | use | valueOf(...), toByteArray() | public class MapWork extends BaseWork |
| 21 | `ql/src/java/org/apache/hadoop/hive/ql/plan/TableScanDesc.java` | use | get(int), size() | Table Scan Descriptor Currently, data is only read from a base source as part |
| 22 | `ql/src/test/org/apache/hadoop/hive/metastore/txn/TestTxnHandler.java` | test |  | Tests for TxnHandler |
| 23 | `ql/src/test/org/apache/hadoop/hive/ql/TestTxnCommands3.java` | test |  | run with and w/o event fitlering enabled - should get the same results |
| 24 | `ql/src/test/org/apache/hadoop/hive/ql/exec/vector/VectorRandomBatchSource.java` | test |  | Generate random batch source from a random Object[] row source (VectorRandomR... |
| 25 | `ql/src/test/org/apache/hadoop/hive/ql/io/TestAcidUtils.java` | test |  | Hive 1.3.0 delta dir naming scheme which supports multi-statement txns |
| 26 | `ql/src/test/org/apache/hadoop/hive/ql/io/orc/TestInputOutputFormat.java` | test |  | Set the blocks and their location for the file |
| 27 | `ql/src/test/org/apache/hadoop/hive/ql/io/orc/TestOrcRawRecordMerger.java` | test |  | {@link org.apache.hive.hcatalog.streaming.TestStreaming#testInterleavedTransa... |
| 28 | `ql/src/test/org/apache/hadoop/hive/ql/io/orc/TestOrcSplit.java` | test |  | Tests for OrcSplit class |
| 29 | `ql/src/test/org/apache/hadoop/hive/ql/io/orc/TestVectorizedOrcAcidRowBatchReader.java` | test |  | This class tests the VectorizedOrcAcidRowBatchReader by creating an actual sp... |
| 30 | `ql/src/test/org/apache/hadoop/hive/ql/optimizer/calcite/rules/views/TestHiveAugmentMaterializationRule.java` | test |  | public class TestHiveAugmentMaterializationRule |
| 31 | `ql/src/test/org/apache/hadoop/hive/ql/parse/TestDMLSemanticAnalyzer.java` | test |  | public class TestDMLSemanticAnalyzer |
| 32 | `serde/src/java/org/apache/hadoop/hive/serde2/thrift/ColumnBuffer.java` | use | BitSet(), get(int), get(from,to), set(from,to), size(), length() | ColumnBuffer |
| 33 | `serde/src/test/org/apache/hadoop/hive/serde2/thrift/TestColumnBuffer.java` | test |  | Test if the nulls BitSet is maintained properly when we extract subset from C... |
| 34 | `service/src/java/org/apache/hive/service/auth/AuthType.java` | use | BitSet(), get(int), set(int), cardinality() | AuthType is used to parse and verify |
| 35 | `standalone-metastore/metastore-common/src/main/java/org/apache/hadoop/hive/metastore/txn/TxnCommonUtils.java` | use | BitSet(), get(int), set(int), valueOf(...) | Transform a {@link org.apache.hadoop.hive.metastore.api.GetOpenTxnsResponse} ... |
| 36 | `standalone-metastore/metastore-common/src/main/java/org/apache/hadoop/hive/metastore/utils/FileUtils.java` | use | BitSet(int), get(int), set(int), size() | Filter that filters out hidden files |
| 37 | `standalone-metastore/metastore-server/src/main/java/org/apache/hadoop/hive/metastore/columnstats/aggr/ColumnStatsAggregator.java` | use | BitSet(int), get(int), set(int), nextClearBit(), cardinality(), size() | The tuner controls the derivation of the NDV value when aggregating statistic... |
| 38 | `standalone-metastore/metastore-server/src/main/java/org/apache/hadoop/hive/metastore/handler/AddPartitionsHandler.java` | use | BitSet() | Validate a partition before creating it. The validation checks |
| 39 | `standalone-metastore/metastore-server/src/main/java/org/apache/hadoop/hive/metastore/handler/CreateTableHandler.java` | use | BitSet() | public class CreateTableHandler |
| 40 | `standalone-metastore/metastore-server/src/main/java/org/apache/hadoop/hive/metastore/txn/TxnUtils.java` | use | BitSet(int), get(int), set(from,to), valueOf(...) | Returns a valid txn list for cleaner |
| 41 | `standalone-metastore/metastore-server/src/main/java/org/apache/hadoop/hive/metastore/txn/entities/OpenTxnList.java` | use | BitSet(), set(int), size(), toByteArray() | Class for the getOpenTxnList calculation |
| 42 | `standalone-metastore/metastore-server/src/main/java/org/apache/hadoop/hive/metastore/txn/jdbc/functions/GetValidWriteIdsForTableFunction.java` | use | BitSet(), set(int), clear(), size(), toByteArray() | public class GetValidWriteIdsForTableFunction |
| 43 | `standalone-metastore/metastore-tools/tools-common/src/main/java/org/apache/hadoop/hive/metastore/tools/HMSClient.java` | use | get(int), valueOf(...) | Wrapper for Thrift HMS interface |
| 44 | `storage-api/src/java/org/apache/hadoop/hive/common/ValidCleanerWriteIdList.java` | use | BitSet() | An implementation of {@link ValidWriteIdList} for use by the Cleaner |
| 45 | `storage-api/src/java/org/apache/hadoop/hive/common/ValidCompactorWriteIdList.java` | use | (тип в сигнатуре) | An implementation of {@link ValidWriteIdList} for use by the compactor |
| 46 | `storage-api/src/java/org/apache/hadoop/hive/common/ValidReadTxnList.java` | use | BitSet(), BitSet(int), get(int), set(int), nextSetBit() | An implementation of {@link org.apache.hadoop.hive.common.ValidTxnList} for u... |
| 47 | `storage-api/src/java/org/apache/hadoop/hive/common/ValidReaderWriteIdList.java` | use | BitSet(), BitSet(int), get(int), set(int), nextSetBit() | An implementation of {@link ValidWriteIdList} for use by readers |
| 48 | `storage-api/src/test/org/apache/hadoop/hive/common/TestValidCompactorWriteIdList.java` | test |  | Tests for {@link ValidCompactorWriteIdList} |
| 49 | `storage-api/src/test/org/apache/hadoop/hive/common/TestValidReadTxnList.java` | test |  | Tests for {@link ValidReadTxnList} |
| 50 | `storage-api/src/test/org/apache/hadoop/hive/common/TestValidReaderWriteIdList.java` | test |  | Tests for {@link ValidReaderWriteIdList} |
| 51 | `storage-api/src/test/org/apache/hive/common/util/TestTxnIdUtils.java` | test |  | public class TestTxnIdUtils |

### Частотная таблица (use-site файлов)

| Метод | Файлов |
|---|---:|
| `BitSet()` | 20 |
| `BitSet(int)` | 11 |
| `get(int)` | 21 |
| `get(from,to)` | 2 |
| `set(int)` | 21 |
| `set(from,to)` | 2 |
| `set(from,to,bool)` | 1 |
| `clear()` | 2 |
| `clear(int)` | 2 |
| `clear(from,to)` | 1 |
| `flip(from,to)` | 1 |
| `and()` | 1 |
| `or()` | 1 |
| `nextSetBit()` | 6 |
| `nextClearBit()` | 2 |
| `previousSetBit()` | 1 |
| `cardinality()` | 7 |
| `size()` | 10 |
| `length()` | 1 |
| `toByteArray()` | 3 |
| `valueOf(...)` | 5 |
| `stream()` | 1 |
| `clone()` | 1 |
| `toString()` | 2 |

**Итого:** 51 файлов (36 use, 15 test)

---

## 17. spring-projects/spring-framework

**Домен:** Web frameworks

### Каталог файлов

| # | Путь | Cls | Методы / Описание | Контекст |
|---|---|---|---|---|
| 1 | `spring-core/src/main/java/org/springframework/cglib/core/EmitUtils.java` | use | BitSet(), get(int), set(int) | Process an array on the stack. Assumes the top item on the stack |
| 2 | `spring-core/src/main/java/org/springframework/util/MimeType.java` | use | BitSet(int), get(int), set(int), set(from,to), andNot() | Represents a MIME Type, as originally defined in RFC 2046 and subsequently |

### Частотная таблица (use-site файлов)

| Метод | Файлов |
|---|---:|
| `BitSet()` | 1 |
| `BitSet(int)` | 1 |
| `get(int)` | 2 |
| `set(int)` | 2 |
| `set(from,to)` | 1 |
| `andNot()` | 1 |

**Итого:** 2 файлов (2 use)

---

## 18. hibernate/hibernate-orm

**Домен:** ORM
**Исключено (false positive):** 1 файлов

### Каталог файлов

| # | Путь | Cls | Методы / Описание | Контекст |
|---|---|---|---|---|
| 1 | `hibernate-core/src/main/java/org/hibernate/engine/spi/ActionQueue.java` | use | BitSet(int), get(int), set(int), nextClearBit() | Responsible for maintaining the queue of actions related to events |
| 2 | `hibernate-core/src/main/java/org/hibernate/internal/util/ImmutableBitSet.java` | impl | Immutable BitSet с defensive copy и contains (subset check); j.u.BitSet мутабелен и не имеет containsAll | An immutable variant of the {@link BitSet} class with some additional operations |
| 3 | `hibernate-core/src/main/java/org/hibernate/internal/util/StringHelper.java` | use | BitSet(), get(int), set(int) | Used to find the ordinal parameters (e.g. '?1') in a string |
| 4 | `hibernate-core/src/main/java/org/hibernate/metamodel/mapping/internal/EmbeddableMappingTypeImpl.java` | use | BitSet(), get(int), set(int) | Describes a "normal" embeddable |
| 5 | `hibernate-core/src/main/java/org/hibernate/metamodel/mapping/internal/ToOneAttributeMapping.java` | use | (тип в сигнатуре) | @author Steve Ebersole |
| 6 | `hibernate-core/src/main/java/org/hibernate/persister/entity/mutation/EntityTableMapping.java` | use | BitSet(), get(int), set(int) | Descriptor for the mapping of a table relative to an entity |
| 7 | `hibernate-core/src/main/java/org/hibernate/persister/entity/mutation/TableSet.java` | use | BitSet(), get(int), set(int) | Represents a Set of TableMapping(s); table mappings are |
| 8 | `hibernate-core/src/main/java/org/hibernate/query/results/internal/complete/EntityResultImpl.java` | use | (тип в сигнатуре) | @author Steve Ebersole |
| 9 | `hibernate-core/src/main/java/org/hibernate/query/sqm/sql/AggregateColumnAssignmentHandler.java` | use | BitSet(int), get(int), set(int), nextSetBit(), previousSetBit(), previousClearBit(), cardinality() | Handler for assignments to sub-columns of an aggregate column, which require ... |
| 10 | `hibernate-core/src/main/java/org/hibernate/query/sqm/sql/internal/SqmMapEntryResult.java` | use | (тип в сигнатуре) | @author Steve Ebersole |
| 11 | `hibernate-core/src/main/java/org/hibernate/sql/ast/spi/AbstractSqlAstTranslator.java` | use | BitSet(int), get(int), set(int), size() | @author Steve Ebersole |
| 12 | `hibernate-core/src/main/java/org/hibernate/sql/results/graph/AbstractFetchParent.java` | use | (тип в сигнатуре) | @author Steve Ebersole |
| 13 | `hibernate-core/src/main/java/org/hibernate/sql/results/graph/BiDirectionalFetch.java` | use | (тип в сигнатуре) | public interface BiDirectionalFetch |
| 14 | `hibernate-core/src/main/java/org/hibernate/sql/results/graph/DomainResultGraphNode.java` | use | (тип в сигнатуре) | Marker for all object types that can be part of a result mapping |
| 15 | `hibernate-core/src/main/java/org/hibernate/sql/results/graph/FetchParent.java` | use | (тип в сигнатуре) | Contract for things that can be the parent of a fetch |
| 16 | `hibernate-core/src/main/java/org/hibernate/sql/results/graph/basic/BasicFetch.java` | use | set(int) | Fetch for a basic-value |
| 17 | `hibernate-core/src/main/java/org/hibernate/sql/results/graph/basic/BasicResult.java` | use | set(int) | DomainResult for a basic-value |
| 18 | `hibernate-core/src/main/java/org/hibernate/sql/results/graph/collection/internal/DelayedCollectionFetch.java` | use | (тип в сигнатуре) | @author Steve Ebersole |
| 19 | `hibernate-core/src/main/java/org/hibernate/sql/results/graph/collection/internal/EagerCollectionFetch.java` | use | (тип в сигнатуре) | @author Steve Ebersole |
| 20 | `hibernate-core/src/main/java/org/hibernate/sql/results/graph/collection/internal/SelectEagerCollectionFetch.java` | use | (тип в сигнатуре) | @author Andrea Boriero |
| 21 | `hibernate-core/src/main/java/org/hibernate/sql/results/graph/entity/AbstractDiscriminatedEntityResultGraphNode.java` | use | (тип в сигнатуре) | abstract class AbstractDiscriminatedEntityResultGraphNode |
| 22 | `hibernate-core/src/main/java/org/hibernate/sql/results/graph/entity/AbstractEntityResultGraphNode.java` | use | (тип в сигнатуре) | AbstractFetchParent sub-class for entity-valued graph nodes |
| 23 | `hibernate-core/src/main/java/org/hibernate/sql/results/graph/entity/EntityFetch.java` | use | (тип в сигнатуре) | Specialization of Fetch for entity-valued fetches |
| 24 | `hibernate-core/src/main/java/org/hibernate/sql/results/graph/entity/internal/AbstractNonJoinedEntityFetch.java` | use | (тип в сигнатуре) | @author Steve Ebersole |
| 25 | `hibernate-core/src/main/java/org/hibernate/sql/results/graph/entity/internal/EntityFetchJoinedImpl.java` | use | (тип в сигнатуре) | @author Andrea Boriero |
| 26 | `hibernate-core/src/main/java/org/hibernate/sql/results/graph/entity/internal/EntityInitializerImpl.java` | use | BitSet(int) | @author Andrea Boriero |
| 27 | `hibernate-core/src/main/java/org/hibernate/sql/results/graph/instantiation/internal/ArgumentDomainResult.java` | use | (тип в сигнатуре) | @author Steve Ebersole |
| 28 | `hibernate-core/src/main/java/org/hibernate/sql/results/graph/instantiation/internal/DynamicInstantiationResultImpl.java` | use | (тип в сигнатуре) | @author Steve Ebersole |
| 29 | `hibernate-core/src/main/java/org/hibernate/sql/results/graph/tuple/TupleResult.java` | use | set(int) | @author Christian Beikov |
| 30 | `hibernate-core/src/main/java/org/hibernate/sql/results/internal/domain/CircularBiDirectionalFetchImpl.java` | use | (тип в сигнатуре) | @author Andrea Boriero |
| 31 | `hibernate-core/src/main/java/org/hibernate/sql/results/jdbc/internal/JdbcValuesResultSetImpl.java` | use | BitSet(int), get(int), set(int), clear() | {@link AbstractJdbcValues} implementation for a JDBC {@link ResultSet} as the... |
| 32 | `hibernate-core/src/main/java/org/hibernate/sql/results/jdbc/internal/StandardJdbcValuesMapping.java` | use | BitSet(int) | @author Steve Ebersole |
| 33 | `hibernate-core/src/main/java/org/hibernate/tuple/entity/EntityMetamodel.java` | use | BitSet(), set(int), isEmpty() | Centralizes metamodel information about an entity |
| 34 | `hibernate-core/src/test/java/org/hibernate/orm/test/mapping/basic/bitset/BitSetConverterImmutableTests.java` | test |  | Test using a converter to map the BitSet |
| 35 | `hibernate-core/src/test/java/org/hibernate/orm/test/mapping/basic/bitset/BitSetConverterMutabilityTests.java` | test |  | Basically the same test as {@link BitSetConverterTests} except here we |
| 36 | `hibernate-core/src/test/java/org/hibernate/orm/test/mapping/basic/bitset/BitSetConverterTests.java` | test |  | Test using a converter to map the BitSet |
| 37 | `hibernate-core/src/test/java/org/hibernate/orm/test/mapping/basic/bitset/BitSetHelper.java` | test |  | @author Steve Ebersole |
| 38 | `hibernate-core/src/test/java/org/hibernate/orm/test/mapping/basic/bitset/BitSetImplicitTests.java` | test |  | Tests for using BitSet without any mapping details |
| 39 | `hibernate-core/src/test/java/org/hibernate/orm/test/mapping/basic/bitset/BitSetJavaType.java` | test |  | @author Vlad Mihalcea |
| 40 | `hibernate-core/src/test/java/org/hibernate/orm/test/mapping/basic/bitset/BitSetJavaTypeContributionTests.java` | test |  | @author Steve Ebersole |
| 41 | `hibernate-core/src/test/java/org/hibernate/orm/test/mapping/basic/bitset/BitSetJavaTypeRegistrationTests.java` | test |  | @author Steve Ebersole |
| 42 | `hibernate-core/src/test/java/org/hibernate/orm/test/mapping/basic/bitset/BitSetJavaTypeTests.java` | test |  | Tests for using an explicit {@link JavaType} |
| 43 | `hibernate-core/src/test/java/org/hibernate/orm/test/mapping/basic/bitset/BitSetJdbcTypeCodeTests.java` | test |  | Tests for {@link JdbcTypeCode} |
| 44 | `hibernate-core/src/test/java/org/hibernate/orm/test/mapping/basic/bitset/BitSetJdbcTypeRegistrationTests.java` | test |  | @author Steve Ebersole |
| 45 | `hibernate-core/src/test/java/org/hibernate/orm/test/mapping/basic/bitset/BitSetJdbcTypeTests.java` | test |  | @author Steve Ebersole |
| 46 | `hibernate-core/src/test/java/org/hibernate/orm/test/mapping/basic/bitset/BitSetMetaUserTypeTest.java` | test |  | public class BitSetMetaUserTypeTest |
| 47 | `hibernate-core/src/test/java/org/hibernate/orm/test/mapping/basic/bitset/BitSetMutabilityPlan.java` | test |  | A BitSet's internal state is mutable, so handle that |
| 48 | `hibernate-core/src/test/java/org/hibernate/orm/test/mapping/basic/bitset/BitSetRegisteredUserTypeTest.java` | test |  | public class BitSetRegisteredUserTypeTest |
| 49 | `hibernate-core/src/test/java/org/hibernate/orm/test/mapping/basic/bitset/BitSetType.java` | test |  | @author Vlad Mihalcea |
| 50 | `hibernate-core/src/test/java/org/hibernate/orm/test/mapping/basic/bitset/BitSetTypeTest.java` | test |  | @author Vlad Mihalcea |
| 51 | `hibernate-core/src/test/java/org/hibernate/orm/test/mapping/basic/bitset/BitSetUserType.java` | test |  | @author Vlad Mihalcea |
| 52 | `hibernate-core/src/test/java/org/hibernate/orm/test/mapping/basic/bitset/BitSetUserTypeTest.java` | test |  | @author Vlad Mihalcea |
| 53 | `hibernate-core/src/test/java/org/hibernate/orm/test/mapping/converted/converter/ConverterOverrideTypeRegisttrationTest.java` | test |  | <pre> |
| 54 | `hibernate-spatial/src/main/java/org/hibernate/spatial/dialect/hana/HANASpatialFunction.java` | use | BitSet(), get(int), set(from,to), size() | public class HANASpatialFunction |
| 55 | `hibernate-vector/src/main/java/org/hibernate/vector/internal/VectorHelper.java` | use | BitSet(), set(int), nextSetBit() | Helper for vector related functionality |
| 56 | `tooling/metamodel-generator/src/main/java/org/hibernate/processor/annotation/ErrorHandler.java` | use | (тип в сигнатуре) | Responsible for forwarding errors from the HQL typechecker to the Java compiler, |

### Частотная таблица (use-site файлов)

| Метод | Файлов |
|---|---:|
| `BitSet()` | 7 |
| `BitSet(int)` | 6 |
| `get(int)` | 9 |
| `set(int)` | 13 |
| `set(from,to)` | 1 |
| `clear()` | 1 |
| `nextSetBit()` | 2 |
| `nextClearBit()` | 1 |
| `previousSetBit()` | 1 |
| `previousClearBit()` | 1 |
| `isEmpty()` | 1 |
| `cardinality()` | 1 |
| `size()` | 2 |

**Итого:** 56 файлов (35 use, 1 impl, 20 test)

---

## 19. RoaringBitmap/RoaringBitmap

**Домен:** Collections / data structures

### Каталог файлов

| # | Путь | Cls | Методы / Описание | Контекст |
|---|---|---|---|---|
| 1 | `fuzz-tests/src/test/java/org/roaringbitmap/BufferFuzzer.java` | test |  | public class BufferFuzzer |
| 2 | `fuzz-tests/src/test/java/org/roaringbitmap/Fuzzer.java` | test |  | public class Fuzzer |
| 3 | `jmh/src/jmh/java/org/roaringbitmap/BitSetUtilBenchmark.java` | test |  | public class BitSetUtilBenchmark |
| 4 | `jmh/src/jmh/java/org/roaringbitmap/iteration/BitmapNextBenchmark.java` | test |  | public class BitmapNextBenchmark |
| 5 | `jmh/src/jmh/java/org/roaringbitmap/iteration/Concatenation.java` | test |  | Using lots and lots of parameter sets is clever, but it takes a long |
| 6 | `jmh/src/jmh/java/org/roaringbitmap/map/MapBenchmark.java` | test |  | public class MapBenchmark |
| 7 | `roaringbitmap/src/main/java/org/roaringbitmap/BitSetUtil.java` | impl | Конвертация j.u.BitSet <-> RoaringBitmap через long[]/byte[]; нет встроенной interop с compressed bitmap форматами | Convert a {@link RoaringBitmap} to a {@link BitSet} |
| 8 | `roaringbitmap/src/main/java/org/roaringbitmap/RoaringBitSet.java` | impl | Drop-in BitSet subclass с compressed RoaringBitmap storage; компенсирует плохую memory efficiency j.u.BitSet на больших sparse наборах | A {@link BitSet} implementation based on {@link RoaringBitmap} |
| 9 | `roaringbitmap/src/main/java/org/roaringbitmap/buffer/BufferBitSetUtil.java` | impl | Конвертация j.u.BitSet <-> off-heap MutableRoaringBitmap; нет поддержки NIO buffer-backed storage | Generate a MutableRoaringBitmap out of a BitSet |
| 10 | `roaringbitmap/src/test/java/org/roaringbitmap/TestBitSetUtil.java` | test |  | public class TestBitSetUtil |
| 11 | `roaringbitmap/src/test/java/org/roaringbitmap/TestRange.java` | test |  | public class TestRange |
| 12 | `roaringbitmap/src/test/java/org/roaringbitmap/TestRoaringBitmap.java` | test |  | Generic testing of the roaring bitmaps |
| 13 | `roaringbitmap/src/test/java/org/roaringbitmap/TestRunContainer.java` | test |  | generates randomly N distinct integers from 0 to Max |
| 14 | `roaringbitmap/src/test/java/org/roaringbitmap/buffer/TestBitSetUtil.java` | test |  | public class TestBitSetUtil |
| 15 | `roaringbitmap/src/test/java/org/roaringbitmap/buffer/TestImmutableRoaringBitmap.java` | test |  | Generic testing of the roaring bitmaps |
| 16 | `roaringbitmap/src/test/java/org/roaringbitmap/buffer/TestRange.java` | test |  | public class TestRange |
| 17 | `roaringbitmap/src/test/java/org/roaringbitmap/buffer/TestRoaringBitmap.java` | test |  | Generic testing of the roaring bitmaps |
| 18 | `roaringbitmap/src/test/java/org/roaringbitmap/buffer/TestRunContainer.java` | test |  | generates randomly N distinct integers from 0 to Max |

**Итого:** 18 файлов (3 impl, 15 test)

---

## 20. apache/commons-lang

**Домен:** JVM core libraries / utilities

### Каталог файлов

| # | Путь | Cls | Методы / Описание | Контекст |
|---|---|---|---|---|
| 1 | `src/main/java/org/apache/commons/lang3/ArrayUtils.java` | use | BitSet(), set(int), nextSetBit(), nextClearBit(), cardinality() | Operations on arrays, primitive arrays (like {@code int[]}) and |
| 2 | `src/main/java/org/apache/commons/lang3/util/FluentBitSet.java` | impl | Fluent API wrapper: mutating-методы возвращают this для chaining; j.u.BitSet возвращает void | A fluent {@link BitSet} with additional operations |
| 3 | `src/test/java/org/apache/commons/lang3/ArrayUtilsTest.java` | test |  | Tests {@link ArrayUtils} |
| 4 | `src/test/java/org/apache/commons/lang3/HashSetvBitSetBenchmark.java` | test |  | Test to show whether using BitSet for removeAll() methods is faster than usin... |
| 5 | `src/test/java/org/apache/commons/lang3/util/FluentBitSetTest.java` | test |  | Tests {@link FluentBitSet} |

### Частотная таблица (use-site файлов)

| Метод | Файлов |
|---|---:|
| `BitSet()` | 1 |
| `set(int)` | 1 |
| `nextSetBit()` | 1 |
| `nextClearBit()` | 1 |
| `cardinality()` | 1 |

**Итого:** 5 файлов (1 use, 1 impl, 3 test)

---

## 21. apache/beam

**Домен:** Data processing / pipeline

### Каталог файлов

| # | Путь | Cls | Методы / Описание | Контекст |
|---|---|---|---|---|
| 1 | `runners/core-java/src/main/java/org/apache/beam/runners/core/triggers/FinishedTriggersBitSet.java` | impl | Домен-специфичная обёртка: isFinished / clearRecursively поверх j.u.BitSet для trigger-state tracking | public class FinishedTriggersBitSet |
| 2 | `runners/core-java/src/main/java/org/apache/beam/runners/core/triggers/TriggerStateMachineRunner.java` | use | isEmpty() | Executes a trigger while managing persistence of information about which subt... |
| 3 | `sdks/java/core/src/main/java/org/apache/beam/sdk/coders/BitSetCoder.java` | impl | Deterministic Coder для сериализации j.u.BitSet в Beam pipelines; нет встроенной streaming serialization | public class BitSetCoder |
| 4 | `sdks/java/core/src/main/java/org/apache/beam/sdk/coders/CoderRegistry.java` | use | (тип в сигнатуре) | A {@link CoderRegistry} allows creating a {@link Coder} for a given Java {@li... |
| 5 | `sdks/java/core/src/main/java/org/apache/beam/sdk/coders/RowCoderGenerator.java` | use | BitSet(int), get(int), set(int) | A utility for automatically generating a {@link Coder} for {@link Row} object... |
| 6 | `sdks/java/core/src/main/java/org/apache/beam/sdk/schemas/transforms/RenameFields.java` | use | BitSet(int), get(int), isEmpty() | A transform for renaming fields inside an existing schema. Top level or neste... |
| 7 | `sdks/java/core/src/main/java/org/apache/beam/sdk/util/BitSetCoder.java` | impl | Deprecated предшественник BitSetCoder; та же gap — отсутствие streaming serialization | Coder for the BitSet used to track child-trigger finished states |
| 8 | `sdks/java/core/src/test/java/org/apache/beam/sdk/coders/BitSetCoderTest.java` | test |  | Generated data to check that the wire format has not changed. To regenerate, ... |
| 9 | `sdks/java/core/src/test/java/org/apache/beam/sdk/schemas/transforms/RenameFieldsTest.java` | test |  | public class RenameFieldsTest |

### Частотная таблица (use-site файлов)

| Метод | Файлов |
|---|---:|
| `BitSet(int)` | 2 |
| `get(int)` | 2 |
| `set(int)` | 1 |
| `isEmpty()` | 2 |

**Итого:** 9 файлов (4 use, 3 impl, 2 test)

---

## 22. elastic/elasticsearch

**Домен:** Search / indexing
**Исключено (false positive):** 28 файлов

### Каталог файлов

| # | Путь | Cls | Методы / Описание | Контекст |
|---|---|---|---|---|
| 1 | `benchmarks/src/main/java/org/elasticsearch/benchmark/compute/operator/BlockBenchmark.java` | test |  | All data type/block kind combinations to be loaded before the benchmark |
| 2 | `modules/lang-painless/src/main/java/org/elasticsearch/painless/ClassWriter.java` | use | (тип в сигнатуре) | Manages the top level writers for class and possibly |
| 3 | `modules/lang-painless/src/main/java/org/elasticsearch/painless/Def.java` | use | BitSet(int), get(int), set(int), length() | Support for dynamic type (def) |
| 4 | `modules/lang-painless/src/main/java/org/elasticsearch/painless/MethodWriter.java` | use | set(int) | Extension of {@link GeneratorAdapter} with some utility methods |
| 5 | `modules/lang-painless/src/main/java/org/elasticsearch/painless/PainlessScript.java` | use | (тип в сигнатуре) | Abstract superclass on top of which all Painless scripts are built |
| 6 | `modules/lang-painless/src/main/java/org/elasticsearch/painless/phase/DefaultIRTreeToASMBytesPhase.java` | use | BitSet(int), length() | public class DefaultIRTreeToASMBytesPhase |
| 7 | `modules/lang-painless/src/main/java/org/elasticsearch/painless/phase/PainlessUserTreeToIRTreePhase.java` | use | (тип в сигнатуре) | public class PainlessUserTreeToIRTreePhase |
| 8 | `server/src/main/java/org/elasticsearch/action/search/SearchQueryThenFetchAsyncAction.java` | use | BitSet(int), get(int), set(int), size() | Response to a query phase request, holding per-shard results that have been p... |
| 9 | `server/src/main/java/org/elasticsearch/common/logging/HeaderWarning.java` | use | BitSet(int), get(int), set(int), length() | This is a simplistic logger that adds warning messages to HTTP headers |
| 10 | `server/src/main/java/org/elasticsearch/lucene/util/automaton/MinimizationOperations.java` | use | BitSet(int), get(int), set(int), clear(int) | Operations for minimizing automata |
| 11 | `server/src/test/java/org/elasticsearch/common/util/ArrayUtilsTests.java` | test |  | public class ArrayUtilsTests |
| 12 | `x-pack/plugin/autoscaling/src/test/java/org/elasticsearch/xpack/autoscaling/AutoscalingTestCase.java` | test |  | abstract class AutoscalingTestCase |
| 13 | `x-pack/plugin/eql/src/main/java/org/elasticsearch/xpack/eql/analysis/Verifier.java` | use | BitSet(int), get(int), set(int), nextSetBit() | The verifier has the role of checking the analyzed tree for failures and buil... |
| 14 | `x-pack/plugin/eql/src/main/java/org/elasticsearch/xpack/eql/parser/EqlParser.java` | use | (тип в сигнатуре) | Parses an EQL statement into execution plan |
| 15 | `x-pack/plugin/eql/src/test/java/org/elasticsearch/xpack/eql/expression/function/scalar/string/BetweenFunctionPipeTests.java` | test |  | public class BetweenFunctionPipeTests |
| 16 | `x-pack/plugin/eql/src/test/java/org/elasticsearch/xpack/eql/expression/function/scalar/string/IndexOfFunctionPipeTests.java` | test |  | public class IndexOfFunctionPipeTests |
| 17 | `x-pack/plugin/eql/src/test/java/org/elasticsearch/xpack/eql/expression/function/scalar/string/SubstringFunctionPipeTests.java` | test |  | public class SubstringFunctionPipeTests |
| 18 | `x-pack/plugin/esql-datasource-ndjson/src/main/java/org/elasticsearch/xpack/esql/datasource/ndjson/NdJsonPageDecoder.java` | use | BitSet(int), get(int), set(int), clear(), size() | public class NdJsonPageDecoder |
| 19 | `x-pack/plugin/esql-datasource-ndjson/src/main/java/org/elasticsearch/xpack/esql/datasource/ndjson/NdJsonSchemaInferrer.java` | use | BitSet(), get(int), set(int), clear() | Infers schema from NDJSON files by reading the first N lines |
| 20 | `x-pack/plugin/esql-datasource-orc/src/main/java/org/elasticsearch/xpack/esql/datasource/orc/OrcFormatReader.java` | use | BitSet(int), set(int) | {@link RangeAwareFormatReader} implementation for Apache ORC files |
| 21 | `x-pack/plugin/esql/arrow/src/main/java/org/elasticsearch/xpack/esql/arrow/BlockConverter.java` | use | BitSet(), BitSet(int), set(int), toByteArray() | Convert a block into Arrow buffers |
| 22 | `x-pack/plugin/esql/compute/src/main/generated-src/org/elasticsearch/compute/data/BooleanArrayBlock.java` | gen |  | Block implementation that stores values in a {@link BooleanArrayVector} |
| 23 | `x-pack/plugin/esql/compute/src/main/generated-src/org/elasticsearch/compute/data/BooleanBigArrayBlock.java` | gen |  | Block implementation that stores values in a {@link BooleanBigArrayVector}. D... |
| 24 | `x-pack/plugin/esql/compute/src/main/generated-src/org/elasticsearch/compute/data/BytesRefArrayBlock.java` | gen |  | Block implementation that stores values in a {@link BytesRefArrayVector} |
| 25 | `x-pack/plugin/esql/compute/src/main/generated-src/org/elasticsearch/compute/data/DoubleArrayBlock.java` | gen |  | Block implementation that stores values in a {@link DoubleArrayVector} |
| 26 | `x-pack/plugin/esql/compute/src/main/generated-src/org/elasticsearch/compute/data/DoubleBigArrayBlock.java` | gen |  | Block implementation that stores values in a {@link DoubleBigArrayVector}. Do... |
| 27 | `x-pack/plugin/esql/compute/src/main/generated-src/org/elasticsearch/compute/data/FloatArrayBlock.java` | gen |  | Block implementation that stores values in a {@link FloatArrayVector} |
| 28 | `x-pack/plugin/esql/compute/src/main/generated-src/org/elasticsearch/compute/data/FloatBigArrayBlock.java` | gen |  | Block implementation that stores values in a {@link FloatBigArrayVector}. Doe... |
| 29 | `x-pack/plugin/esql/compute/src/main/generated-src/org/elasticsearch/compute/data/IntArrayBlock.java` | gen |  | Block implementation that stores values in a {@link IntArrayVector} |
| 30 | `x-pack/plugin/esql/compute/src/main/generated-src/org/elasticsearch/compute/data/IntBigArrayBlock.java` | gen |  | Block implementation that stores values in a {@link IntBigArrayVector}. Does ... |
| 31 | `x-pack/plugin/esql/compute/src/main/generated-src/org/elasticsearch/compute/data/LongArrayBlock.java` | gen |  | Block implementation that stores values in a {@link LongArrayVector} |
| 32 | `x-pack/plugin/esql/compute/src/main/generated-src/org/elasticsearch/compute/data/LongBigArrayBlock.java` | gen |  | Block implementation that stores values in a {@link LongBigArrayVector}. Does... |
| 33 | `x-pack/plugin/esql/compute/src/main/java/org/elasticsearch/compute/data/AbstractArrayBlock.java` | use | BitSet(int), get(int), set(int), nextSetBit(), isEmpty(), cardinality(), size(), toLongArray(), valueOf(...) | @param positionCount the number of values in this block |
| 34 | `x-pack/plugin/esql/compute/src/main/java/org/elasticsearch/compute/data/AbstractBlockBuilder.java` | use | BitSet(), set(int) | Called during implementations of {@link Block.Builder#build} as a first step |
| 35 | `x-pack/plugin/esql/compute/src/main/java/org/elasticsearch/compute/data/BlockFactory.java` | use | (тип в сигнатуре) | {@return a builder for constructing a {@link BlockFactory}} |
| 36 | `x-pack/plugin/esql/compute/src/main/java/org/elasticsearch/compute/data/BlockRamUsageEstimator.java` | use | size() | final class BlockRamUsageEstimator |
| 37 | `x-pack/plugin/esql/compute/src/test/java/org/elasticsearch/compute/data/BasicBlockTests.java` | test |  | Take an object with exactly 1 reference and assert that ref counting works fine |
| 38 | `x-pack/plugin/esql/compute/src/test/java/org/elasticsearch/compute/data/BlockAccountingTests.java` | test |  | Ideally we would test a real Block builder, but that would require a large he... |
| 39 | `x-pack/plugin/esql/compute/src/test/java/org/elasticsearch/compute/data/BlockFactoryTests.java` | test |  | public class BlockFactoryTests |
| 40 | `x-pack/plugin/esql/compute/src/test/java/org/elasticsearch/compute/data/BlockTypeRandomizer.java` | test |  | Returns a block with the same contents, but with a randomized type (Constant,... |
| 41 | `x-pack/plugin/esql/compute/src/test/java/org/elasticsearch/compute/data/BooleanBlockEqualityTests.java` | test |  | public class BooleanBlockEqualityTests |
| 42 | `x-pack/plugin/esql/compute/src/test/java/org/elasticsearch/compute/data/BytesRefBlockEqualityTests.java` | test |  | public class BytesRefBlockEqualityTests |
| 43 | `x-pack/plugin/esql/compute/src/test/java/org/elasticsearch/compute/data/DoubleBlockEqualityTests.java` | test |  | public class DoubleBlockEqualityTests |
| 44 | `x-pack/plugin/esql/compute/src/test/java/org/elasticsearch/compute/data/FilteredBlockTests.java` | test |  | public class FilteredBlockTests |
| 45 | `x-pack/plugin/esql/compute/src/test/java/org/elasticsearch/compute/data/FloatBlockEqualityTests.java` | test |  | public class FloatBlockEqualityTests |
| 46 | `x-pack/plugin/esql/compute/src/test/java/org/elasticsearch/compute/data/IntBlockEqualityTests.java` | test |  | public class IntBlockEqualityTests |
| 47 | `x-pack/plugin/esql/compute/src/test/java/org/elasticsearch/compute/data/LongBlockEqualityTests.java` | test |  | public class LongBlockEqualityTests |
| 48 | `x-pack/plugin/esql/compute/src/test/java/org/elasticsearch/compute/operator/MMROperatorTests.java` | test |  | public class MMROperatorTests |
| 49 | `x-pack/plugin/esql/compute/test/src/main/java/org/elasticsearch/compute/test/MockBlockFactory.java` | test |  | A block factory that tracks the creation of blocks, vectors, builders that it... |
| 50 | `x-pack/plugin/esql/src/main/generated-src/org/elasticsearch/xpack/esql/expression/predicate/operator/comparison/InBooleanEvaluator.java` | gen |  | {@link ExpressionEvaluator} implementation for {@link In} |
| 51 | `x-pack/plugin/esql/src/main/generated-src/org/elasticsearch/xpack/esql/expression/predicate/operator/comparison/InBytesRefEvaluator.java` | gen |  | {@link ExpressionEvaluator} implementation for {@link In} |
| 52 | `x-pack/plugin/esql/src/main/generated-src/org/elasticsearch/xpack/esql/expression/predicate/operator/comparison/InDoubleEvaluator.java` | gen |  | {@link ExpressionEvaluator} implementation for {@link In} |
| 53 | `x-pack/plugin/esql/src/main/generated-src/org/elasticsearch/xpack/esql/expression/predicate/operator/comparison/InIntEvaluator.java` | gen |  | {@link ExpressionEvaluator} implementation for {@link In} |
| 54 | `x-pack/plugin/esql/src/main/generated-src/org/elasticsearch/xpack/esql/expression/predicate/operator/comparison/InLongEvaluator.java` | gen |  | {@link ExpressionEvaluator} implementation for {@link In} |
| 55 | `x-pack/plugin/esql/src/main/generated-src/org/elasticsearch/xpack/esql/expression/predicate/operator/comparison/InMillisNanosEvaluator.java` | gen |  | {@link ExpressionEvaluator} implementation for {@link In} |
| 56 | `x-pack/plugin/esql/src/main/generated-src/org/elasticsearch/xpack/esql/expression/predicate/operator/comparison/InNanosMillisEvaluator.java` | gen |  | {@link ExpressionEvaluator} implementation for {@link In} |
| 57 | `x-pack/plugin/esql/src/main/java/org/elasticsearch/xpack/esql/analysis/Analyzer.java` | use | BitSet(int), set(int) | This class is part of the planner. Resolves references (such as variable and ... |
| 58 | `x-pack/plugin/esql/src/main/java/org/elasticsearch/xpack/esql/analysis/Verifier.java` | use | nextSetBit() | This class is part of the planner. Responsible for failing impossible queries... |
| 59 | `x-pack/plugin/esql/src/main/java/org/elasticsearch/xpack/esql/core/tree/NodeToString.java` | use | BitSet(), get(int), set(from,to), size() | Renders {@link Node} trees as strings |
| 60 | `x-pack/plugin/esql/src/main/java/org/elasticsearch/xpack/esql/datasources/spi/ColumnBlockConversions.java` | use | BitSet(int), set(int) | Converts columnar primitive arrays to ESQL {@link Block}s, avoiding the per-e... |
| 61 | `x-pack/plugin/esql/src/main/java/org/elasticsearch/xpack/esql/expression/predicate/operator/comparison/In.java` | use | get(int) | The {@code IN} operator |
| 62 | `x-pack/plugin/esql/src/main/java/org/elasticsearch/xpack/esql/parser/EsqlParser.java` | use | BitSet(int), get(int), set(int), cardinality() | Maximum number of characters in an ESQL query. Antlr may parse the entire |
| 63 | `x-pack/plugin/esql/src/main/java/org/elasticsearch/xpack/esql/parser/PromqlParser.java` | use | (тип в сигнатуре) | Parses an PromQL expression into execution plan |
| 64 | `x-pack/plugin/esql/src/main/java/org/elasticsearch/xpack/esql/telemetry/FeatureMetric.java` | use | set(int) | ESQL "features" returned by the usage API. This <strong>mostly</strong> tracks |
| 65 | `x-pack/plugin/ml/src/main/java/org/elasticsearch/xpack/ml/aggs/frequentitemsets/CountingItemSetTraverser.java` | use | BitSet(), BitSet(int), get(int), set(int), clear(int) | Item set traverser to find the next interesting item set |
| 66 | `x-pack/plugin/old-lucene-versions/src/main/java/org/elasticsearch/xpack/lucene/bwc/codecs/lucene70/fst/Util.java` | use | BitSet(), get(int), set(int) | Static helper methods |
| 67 | `x-pack/plugin/ql/src/main/java/org/elasticsearch/xpack/ql/tree/Node.java` | use | BitSet(), get(int), set(from,to), size(), toString() | Immutable tree structure |
| 68 | `x-pack/plugin/ql/src/test/java/org/elasticsearch/xpack/ql/expression/function/scalar/FunctionTestUtils.java` | test |  | final class FunctionTestUtils |
| 69 | `x-pack/plugin/ql/src/test/java/org/elasticsearch/xpack/ql/expression/function/scalar/string/StartsWithFunctionPipeTests.java` | test |  | public class StartsWithFunctionPipeTests |
| 70 | `x-pack/plugin/sql/src/main/java/org/elasticsearch/xpack/sql/analysis/analyzer/Verifier.java` | use | BitSet(int), set(int), nextSetBit() | The verifier has the role of checking the analyzed tree for failures and buil... |
| 71 | `x-pack/plugin/sql/src/main/java/org/elasticsearch/xpack/sql/execution/search/CompositeAggCursor.java` | use | toByteArray(), valueOf(...) | Cursor for composite aggregation (GROUP BY) |
| 72 | `x-pack/plugin/sql/src/main/java/org/elasticsearch/xpack/sql/execution/search/CompositeAggRowSet.java` | use | (тип в сигнатуре) | {@link RowSet} specific to (GROUP BY) aggregation |
| 73 | `x-pack/plugin/sql/src/main/java/org/elasticsearch/xpack/sql/execution/search/PivotCursor.java` | use | (тип в сигнатуре) | public class PivotCursor |
| 74 | `x-pack/plugin/sql/src/main/java/org/elasticsearch/xpack/sql/execution/search/PivotRowSet.java` | use | (тип в сигнатуре) | PivotRowSet.java |
| 75 | `x-pack/plugin/sql/src/main/java/org/elasticsearch/xpack/sql/execution/search/Querier.java` | use | nextSetBit(), cardinality() | Refreshes the PIT ID on the search source with the new value returned in the ... |
| 76 | `x-pack/plugin/sql/src/main/java/org/elasticsearch/xpack/sql/execution/search/ResultRowSet.java` | use | nextSetBit(), cardinality(), size(), length() | abstract class ResultRowSet |
| 77 | `x-pack/plugin/sql/src/main/java/org/elasticsearch/xpack/sql/execution/search/SchemaCompositeAggRowSet.java` | use | (тип в сигнатуре) | Extension of the {@link RowSet} over a composite agg, extending it to provide... |
| 78 | `x-pack/plugin/sql/src/main/java/org/elasticsearch/xpack/sql/execution/search/SchemaSearchHitRowSet.java` | use | (тип в сигнатуре) | Initial results from a search hit search. Distinct from the following pages |
| 79 | `x-pack/plugin/sql/src/main/java/org/elasticsearch/xpack/sql/execution/search/SearchHitCursor.java` | use | toByteArray(), valueOf(...) | public class SearchHitCursor |
| 80 | `x-pack/plugin/sql/src/main/java/org/elasticsearch/xpack/sql/execution/search/SearchHitRowSet.java` | use | (тип в сигнатуре) | Extracts rows from an array of {@link SearchHit} |
| 81 | `x-pack/plugin/sql/src/main/java/org/elasticsearch/xpack/sql/parser/SqlParser.java` | use | (тип в сигнатуре) | Used only in tests |
| 82 | `x-pack/plugin/sql/src/main/java/org/elasticsearch/xpack/sql/querydsl/container/QueryContainer.java` | use | BitSet(int), get(int), set(int), size(), equals() | Container for various references of the built ES query |
| 83 | `x-pack/plugin/sql/src/test/java/org/elasticsearch/xpack/sql/execution/search/CompositeAggregationCursorTests.java` | test |  | public class CompositeAggregationCursorTests |
| 84 | `x-pack/plugin/sql/src/test/java/org/elasticsearch/xpack/sql/execution/search/QuerierTests.java` | test |  | public class QuerierTests |
| 85 | `x-pack/plugin/sql/src/test/java/org/elasticsearch/xpack/sql/execution/search/SearchHitCursorTests.java` | test |  | public class SearchHitCursorTests |
| 86 | `x-pack/plugin/sql/src/test/java/org/elasticsearch/xpack/sql/expression/function/scalar/string/InsertFunctionPipeTests.java` | test |  | public class InsertFunctionPipeTests |
| 87 | `x-pack/plugin/sql/src/test/java/org/elasticsearch/xpack/sql/expression/function/scalar/string/LocateFunctionPipeTests.java` | test |  | public class LocateFunctionPipeTests |
| 88 | `x-pack/plugin/sql/src/test/java/org/elasticsearch/xpack/sql/expression/function/scalar/string/ReplaceFunctionPipeTests.java` | test |  | public class ReplaceFunctionPipeTests |
| 89 | `x-pack/plugin/sql/src/test/java/org/elasticsearch/xpack/sql/expression/function/scalar/string/SubstringFunctionPipeTests.java` | test |  | public class SubstringFunctionPipeTests |
| 90 | `x-pack/plugin/sql/src/test/java/org/elasticsearch/xpack/sql/querydsl/container/QueryContainerTests.java` | test |  | public class QueryContainerTests |
| 91 | `x-pack/plugin/text-structure/src/main/java/org/elasticsearch/xpack/textstructure/structurefinder/DelimitedTextStructureFinder.java` | use | BitSet(), set(int), set(from,to), nextSetBit() | Make a mask whose bits are set when the corresponding field in every supplied |
| 92 | `x-pack/plugin/text-structure/src/main/java/org/elasticsearch/xpack/textstructure/structurefinder/TimestampFormatFinder.java` | use | BitSet(), get(int), set(int), isEmpty(), cardinality(), length(), stream() | Used to find the best timestamp format for one of the following situations: |
| 93 | `x-pack/plugin/text-structure/src/test/java/org/elasticsearch/xpack/textstructure/structurefinder/DelimitedTextStructureFinderTests.java` | test |  | public class DelimitedTextStructureFinderTests |
| 94 | `x-pack/plugin/text-structure/src/test/java/org/elasticsearch/xpack/textstructure/structurefinder/TimestampFormatFinderTests.java` | test |  | Logic copied from {@code org.elasticsearch.ingest.common.DateFormat.Tai64n.pa... |
| 95 | `x-pack/plugin/watcher/src/test/java/org/elasticsearch/xpack/watcher/WatcherIndexingListenerTests.java` | test |  | create a mock cluster state, the returns the specified index as watch index |
| 96 | `x-pack/plugin/watcher/src/test/java/org/elasticsearch/xpack/watcher/trigger/schedule/engine/TickerScheduleEngineTests.java` | test |  | When a cluster state changes and watches are restarted, the engine should cle... |

### Частотная таблица (use-site файлов)

| Метод | Файлов |
|---|---:|
| `BitSet()` | 9 |
| `BitSet(int)` | 16 |
| `get(int)` | 16 |
| `set(int)` | 22 |
| `set(from,to)` | 3 |
| `clear()` | 2 |
| `clear(int)` | 2 |
| `nextSetBit()` | 7 |
| `isEmpty()` | 2 |
| `cardinality()` | 5 |
| `size()` | 8 |
| `length()` | 5 |
| `toByteArray()` | 3 |
| `toLongArray()` | 1 |
| `valueOf(...)` | 3 |
| `stream()` | 1 |
| `equals()` | 1 |
| `toString()` | 1 |

**Итого:** 96 файлов (45 use, 33 test, 18 gen)

---

## 23. apache/cassandra

**Домен:** Database engines

### Каталог файлов

| # | Путь | Cls | Методы / Описание | Контекст |
|---|---|---|---|---|
| 1 | `src/java/org/apache/cassandra/hints/HintsDispatcher.java` | use | BitSet(int), get(int), set(int), size() | Dispatches a single hints file to a specified node in a batched manner |
| 2 | `src/java/org/apache/cassandra/metrics/ThreadLocalMeter.java` | use | BitSet(), set(int), clear(int), nextSetBit() | An alternative to Dropwizard Meter which implements the same kind of API |
| 3 | `src/java/org/apache/cassandra/metrics/ThreadLocalMetrics.java` | use | BitSet(), set(int), clear(int), nextSetBit(), cardinality() | A thread-local counter implementation designed to use in metrics as an altern... |
| 4 | `src/java/org/apache/cassandra/service/reads/repair/RowIteratorMergeListener.java` | use | BitSet(int), get(int), set(int) | The partition level deletion with with which source {@code i} is currently re... |
| 5 | `src/java/org/apache/cassandra/utils/MurmurHash.java` | use | flip(int), toByteArray(), valueOf(...) | This is a very fast, non-cryptographic hash suitable for general hash-based |
| 6 | `test/burn/org/apache/cassandra/concurrent/LongSharedExecutorPoolTest.java` | test |  | public class LongSharedExecutorPoolTest |
| 7 | `test/burn/org/apache/cassandra/net/Reporters.java` | test |  | final class OneTimeUnit |
| 8 | `test/distributed/org/apache/cassandra/distributed/test/accord/AccordLoadTest.java` | test |  | public class AccordLoadTest |
| 9 | `test/harry/main/org/apache/cassandra/harry/model/ASTSingleTableModel.java` | test |  | The common case there can only be 1 value, but in the case of {@link Conditio... |
| 10 | `test/long/org/apache/cassandra/dht/tokenallocator/AbstractReplicationAwareTokenAllocatorTest.java` | test |  | Base class for {@link Murmur3ReplicationAwareTokenAllocatorTest} and {@link R... |
| 11 | `test/microbench/org/apache/cassandra/test/microbench/btree/BTreeTransformBench.java` | test |  | public class BTreeTransformBench |
| 12 | `test/simulator/main/org/apache/cassandra/simulator/systems/SimulatedFutureActionScheduler.java` | test |  | public class SimulatedFutureActionScheduler |
| 13 | `test/unit/org/apache/cassandra/concurrent/ManyToOneConcurrentLinkedQueueTest.java` | test |  | public class ManyToOneConcurrentLinkedQueueTest |
| 14 | `test/unit/org/apache/cassandra/db/compaction/unified/BackgroundCompactionTrackingTest.java` | test |  | public class BackgroundCompactionTrackingTest |

### Частотная таблица (use-site файлов)

| Метод | Файлов |
|---|---:|
| `BitSet()` | 2 |
| `BitSet(int)` | 2 |
| `get(int)` | 2 |
| `set(int)` | 4 |
| `clear(int)` | 2 |
| `flip(int)` | 1 |
| `nextSetBit()` | 2 |
| `cardinality()` | 1 |
| `size()` | 1 |
| `toByteArray()` | 1 |
| `valueOf(...)` | 1 |

**Итого:** 14 файлов (5 use, 9 test)

---

## Глобальная сводка

| Метрика | Значение |
|---|---:|
| Репозиториев | 23 |
| Файлов (после фильтрации) | 641 |
| Исключено (false positive) | 51 |
| use | 422 |
| impl | 23 |
| test | 178 |
| gen | 18 |

### Глобальная частотная таблица (use-site файлов)

| Метод | Файлов |
|---|---:|
| `BitSet()` | 168 |
| `BitSet(int)` | 119 |
| `get(int)` | 253 |
| `get(from,to)` | 3 |
| `set(int)` | 243 |
| `set(int,bool)` | 12 |
| `set(from,to)` | 31 |
| `set(from,to,bool)` | 6 |
| `clear()` | 33 |
| `clear(int)` | 40 |
| `clear(from,to)` | 5 |
| `flip(int)` | 1 |
| `flip(from,to)` | 1 |
| `and()` | 15 |
| `or()` | 36 |
| `andNot()` | 14 |
| `nextSetBit()` | 67 |
| `nextClearBit()` | 13 |
| `previousSetBit()` | 5 |
| `previousClearBit()` | 1 |
| `isEmpty()` | 39 |
| `cardinality()` | 54 |
| `size()` | 48 |
| `length()` | 15 |
| `intersects()` | 2 |
| `toByteArray()` | 12 |
| `toLongArray()` | 3 |
| `valueOf(...)` | 15 |
| `stream()` | 7 |
| `clone()` | 19 |
| `equals()` | 17 |
| `hashCode()` | 2 |
| `toString()` | 5 |

### Арифметическая проверка

| Repo | use | impl | test | gen | total |
|---|---:|---:|---:|---:|---:|
| antlr/antlr4 | 12 | 0 | 1 | 0 | 13 |
| oracle/graal | 94 | 4 | 5 | 0 | 103 |
| apache/lucene | 13 | 0 | 19 | 0 | 32 |
| androidx/androidx | 4 | 0 | 3 | 0 | 7 |
| google/guava | 6 | 2 | 8 | 0 | 16 |
| apache/spark | 1 | 0 | 2 | 0 | 3 |
| spotbugs/spotbugs | 65 | 2 | 0 | 0 | 67 |
| apache/calcite | 16 | 2 | 3 | 0 | 21 |
| h2database/h2database | 16 | 1 | 6 | 0 | 23 |
| checkstyle/checkstyle | 24 | 0 | 11 | 0 | 35 |
| pmd/pmd | 3 | 0 | 0 | 0 | 3 |
| apache/flink | 19 | 0 | 9 | 0 | 28 |
| netty/netty | 4 | 1 | 0 | 0 | 5 |
| eclipse-collections/eclipse-collections | 4 | 0 | 2 | 0 | 6 |
| apache/druid | 13 | 3 | 12 | 0 | 28 |
| apache/hive | 36 | 0 | 15 | 0 | 51 |
| spring-projects/spring-framework | 2 | 0 | 0 | 0 | 2 |
| hibernate/hibernate-orm | 35 | 1 | 20 | 0 | 56 |
| RoaringBitmap/RoaringBitmap | 0 | 3 | 15 | 0 | 18 |
| apache/commons-lang | 1 | 1 | 3 | 0 | 5 |
| apache/beam | 4 | 3 | 2 | 0 | 9 |
| elastic/elasticsearch | 45 | 0 | 33 | 18 | 96 |
| apache/cassandra | 5 | 0 | 9 | 0 | 14 |
| **Итого** | **422** | **23** | **178** | **18** | **641** |

#### Верификация per-repo сумм

Сумма per-repo use-site counts по каждому методу совпадает с глобальной таблицей. Расхождений не обнаружено.
