# Шаг 3c, часть 1. Частотная таблица методов BitSet

## Резюме

Построена объединённая частотная таблица по 178 use-site entries: 20 из репозитория Kotlin и 158 из IntelliJ TSV. В суммарной выборке доминируют `get(int)` (75), `set(from,to)` (63), `BitSet()` (60), `set(int)` (52) и `BitSet(int)` (33).

Интеллиевские частоты полностью раскладываются на `J` и `W`: непустых `mode=?` строк нет. Kotlin-часть добавляет операции, которых почти нет или совсем нет в IntelliJ-выборке: `copy()`, `forEachBit()`, `mapEachBit()`, `valueOf(LongArray)`, `set(IntRange)`, `orWithFilterHasChanged()`.

## Входные данные

- `bitset-research/step-03a-kotlin-repo-data.md`
- `bitset-research/step-03b-final.tsv`

## Scope

- IntelliJ TSV: `158` use-site entries (`cls=use`). Из них `129` имеют непустой `methods`, `29` — pass-through entries с пустым `methods`.
- Колонки IntelliJ в таблицах: `IntelliJ-J` = `mode=J` (`JVM-direct`, прямое использование `java.util.BitSet`), `IntelliJ-W` = `mode=W` (`wrapper-mediated`, использование через обёртку или кастомный BitSet-тип). Колонка `IntelliJ` — их сумма.
- Kotlin `step-03a`: `20` use-site файлов после исключения `4` реализаций BitSet, `1` test suite и `1` API dump. Из них `18` имеют методы, `2` — pass-through.
- Комбинированная выборка: `178` use-site entries, из них `147` реально вносят вклад в частоты методов.
- Нормализация `step-03a` к TSV-словарю: `new BitSet(int)` / `BitSet(size)` / `CustomBitSet(nodesCount)` -> `BitSet(int)`; `CustomBitSet()` -> `BitSet()`; `[int]` -> `get(int)`; `[int] = ...` и `.set(int, Boolean)` -> `set(int,bool)`; свойства `.isEmpty` / `.size` -> `isEmpty()` / `size()`; `CustomBitSet.valueOf(LongArray)` -> `valueOf(LongArray)`.

## Kotlin Per-File Method Lists

| File | Normalized methods |
|---|---|
| `compiler/util/src/org/jetbrains/kotlin/utils/BitSetUtil.kt` | `BitSet(int)`, `size()`, `or()`, `nextSetBit()` |
| `compiler/psi/parser/src/org/jetbrains/kotlin/kdoc/lexer/_KDocLexer.java` | `BitSet(int)`, `set(int,bool)`, `clear(int)`, `get(int)`, `size()` |
| `compiler/backend/src/org/jetbrains/kotlin/codegen/coroutines/resumePointDependentAnalysis.kt` | `BitSet(int)`, `set(int)`, `get(int)`, `clear()`, `or()`, `copy()`, `forEachBit()`, `equals()`, `toString()` |
| `compiler/backend/src/org/jetbrains/kotlin/codegen/optimization/common/variableLiveness.kt` | `BitSet(int)`, `or()`, `set(int,bool)`, `get(int)`, `equals()`, `hashCode()` |
| `compiler/backend/src/org/jetbrains/kotlin/codegen/optimization/boxing/PopBackwardPropagationTransformer.kt` | `BitSet(int)`, `set(int,bool)`, `get(int)` |
| `compiler/ir/backend.common/src/org/jetbrains/kotlin/backend/common/lower/optimizations/LivenessAnalysis.kt` | `BitSet()`, `copy()`, `forEachBit()`, `or()`, `andNot()`, `set(int)`, `get(int)`, `clear(int)` |
| `kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/optimizations/ComputeTypesPass.kt` | `BitSet()`, `set(int)`, `and()`, `or()`, `andNot()`, `copy()`, `mapEachBit()`, `forEachBit()` |
| `kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/optimizations/StaticInitializersOptimization.kt` | `BitSet()`, `or()`, `and()`, `copy()`, `set(int)`, `get(int)`, `cardinality()` |
| `kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/optimizations/DataFlowIR.kt` | `BitSet()`, `set(int)`, `get(int)`, `isEmpty()`, `or()` |
| `kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/optimizations/DevirtualizationAnalysis.kt` | `BitSet()`, `BitSet(int)`, `set(int)`, `get(int)`, `set(int,bool)`, `clear()`, `cardinality()`, `forEachBit()`, `copy()`, `or()`, `and()`, `equals()`, `orWithFilterHasChanged()` |
| `kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/optimizations/CastsOptimization.kt` | `BitSet()`, `valueOf(LongArray)`, `set(int)`, `cardinality()`, `intersects()`, `copy()`, `or()`, `andNot()`, `forEachBit()`, `forEachWord()`, `size()` |
| `kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/optimizations/CallGraphBuilder.kt` | `forEachBit()` |
| `analysis/symbol-light-classes/src/org/jetbrains/kotlin/light/classes/symbol/classes/symbolLightClassUtils.kt` | `BitSet(int)`, `set(int)`, `set(from,to)`, `isEmpty()`, `clear(int)`, `get(int)`, `copy()`, `intersects()`, `length()`, `clone()`, `equals()`, `hashCode()` |
| `analysis/symbol-light-classes/src/org/jetbrains/kotlin/light/classes/symbol/symbolLightUtils.kt` | `clone()` |
| `analysis/symbol-light-classes/src/org/jetbrains/kotlin/light/classes/symbol/methods/SymbolLightMethod.kt` | `get(int)`, `equals()` |
| `analysis/symbol-light-classes/src/org/jetbrains/kotlin/light/classes/symbol/methods/SymbolLightSimpleMethod.kt` | pass-through |
| `analysis/symbol-light-classes/src/org/jetbrains/kotlin/light/classes/symbol/methods/SymbolLightConstructor.kt` | pass-through |
| `libraries/stdlib/native-wasm/src/kotlin/text/regex/CharClass.kt` | `BitSet()`, `set(int,bool)`, `set(from,to,bool)`, `get(int)`, `xor()`, `and()`, `or()`, `andNot()`, `isEmpty()`, `nextSetBit()` |
| `libraries/stdlib/native-wasm/src/kotlin/text/regex/AbstractCharClass.kt` | `BitSet(int)`, `nextClearBit()`, `nextSetBit()`, `get(int)`, `set(IntRange)`, `set(from,to)`, `intersects()` |
| `native/commonizer/src/org/jetbrains/kotlin/commonizer/stats/RawStatsCollector.kt` | `BitSet(int)`, `set(int,bool)`, `get(int)` |

## Frequency Table

### Construction

| Method | Kotlin | IntelliJ | IntelliJ-J | IntelliJ-W | Total |
|---|---:|---:|---:|---:|---:|
| `BitSet()` | 7 | 53 | 50 | 3 | 60 |
| `BitSet(int)` | 9 | 24 | 18 | 6 | 33 |
| `valueOf(LongArray)` | 1 | 0 | 0 | 0 | 1 |

Traceability:
- `BitSet()` (60) — representative files: K: `compiler/ir/backend.common/src/org/jetbrains/kotlin/backend/common/lower/optimizations/LivenessAnalysis.kt`; IJ-J: `grid/impl/src/run/ui/HiddenColumnsSelectionHolder.java`; IJ-W: `platform/syntax/syntax-api/src/com/intellij/platform/syntax/impl/builder/MarkerOptionalData.kt`
- `BitSet(int)` (33) — representative files: K: `analysis/symbol-light-classes/src/org/jetbrains/kotlin/light/classes/symbol/classes/symbolLightClassUtils.kt`; IJ-J: `java/idea-ui/src/com/intellij/ide/util/importProject/RootDetectionProcessor.java`; IJ-W: `fleet/util/core/srcCommonMain/fleet/util/BitSet.kt`
- `valueOf(LongArray)` (1) — K: `kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/optimizations/CastsOptimization.kt`

### Single-bit

| Method | Kotlin | IntelliJ | IntelliJ-J | IntelliJ-W | Total |
|---|---:|---:|---:|---:|---:|
| `get(int)` | 13 | 62 | 44 | 18 | 75 |
| `set(int)` | 8 | 44 | 34 | 10 | 52 |
| `set(int,bool)` | 6 | 26 | 16 | 10 | 32 |
| `clear(int)` | 3 | 6 | 3 | 3 | 9 |

Traceability:
- `get(int)` (75) — representative files: K: `analysis/symbol-light-classes/src/org/jetbrains/kotlin/light/classes/symbol/classes/symbolLightClassUtils.kt`; IJ-J: `grid/impl/src/run/ui/HiddenColumnsSelectionHolder.java`; IJ-W: `platform/lang-api/src/com/intellij/util/indexing/LightDirectoryIndex.java`
- `set(int)` (52) — representative files: K: `analysis/symbol-light-classes/src/org/jetbrains/kotlin/light/classes/symbol/classes/symbolLightClassUtils.kt`; IJ-J: `grid/impl/src/run/ui/HiddenColumnsSelectionHolder.java`; IJ-W: `platform/indexing-api/src/com/intellij/util/indexing/roots/IndexableFilesDeduplicateFilter.java`
- `set(int,bool)` (32) — representative files: K: `compiler/backend/src/org/jetbrains/kotlin/codegen/optimization/boxing/PopBackwardPropagationTransformer.kt`; IJ-J: `java/idea-ui/src/com/intellij/ide/util/importProject/RootDetectionProcessor.java`; IJ-W: `platform/lang-impl/src/com/intellij/util/indexing/projectFilter/ConcurrentFileIds.kt`
- `clear(int)` (9) — K: `analysis/symbol-light-classes/src/org/jetbrains/kotlin/light/classes/symbol/classes/symbolLightClassUtils.kt`, `compiler/ir/backend.common/src/org/jetbrains/kotlin/backend/common/lower/optimizations/LivenessAnalysis.kt`, `compiler/psi/parser/src/org/jetbrains/kotlin/kdoc/lexer/_KDocLexer.java`; IJ-J: `grid/impl/src/run/ui/HiddenColumnsSelectionHolder.java`, `platform/analysis-impl/src/com/intellij/codeInspection/dataFlow/memory/DfaMemoryStateImpl.java`, `platform/core-impl/src/com/intellij/openapi/vfs/CompactVirtualFileSet.java`; IJ-W: `platform/lang-impl/src/com/intellij/util/indexing/events/DirtyFiles.kt`, `platform/platform-impl/src/com/intellij/openapi/fileTypes/impl/IgnoredFileCache.java`, `platform/todo/src/com/intellij/psi/impl/cache/impl/IndexTodoCacheManagerImpl.java`

### Range

| Method | Kotlin | IntelliJ | IntelliJ-J | IntelliJ-W | Total |
|---|---:|---:|---:|---:|---:|
| `set(from,to)` | 2 | 61 | 49 | 12 | 63 |
| `set(from,to,bool)` | 1 | 5 | 3 | 2 | 6 |
| `set(IntRange)` | 1 | 0 | 0 | 0 | 1 |

Traceability:
- `set(from,to)` (63) — representative files: K: `analysis/symbol-light-classes/src/org/jetbrains/kotlin/light/classes/symbol/classes/symbolLightClassUtils.kt`; IJ-J: `java/idea-ui/src/com/intellij/ide/util/importProject/RootDetectionProcessor.java`; IJ-W: `platform/lang-impl/src/com/intellij/util/indexing/projectFilter/ConcurrentFileIds.kt`
- `set(from,to,bool)` (6) — K: `libraries/stdlib/native-wasm/src/kotlin/text/regex/CharClass.kt`; IJ-J: `platform/diff-impl/src/com/intellij/diff/tools/combined/CombinedDiffViewer.kt`, `platform/vcs-impl/src/com/intellij/openapi/vcs/changes/patch/AppliedTextPatch.java`, `platform/vcs-impl/src/com/intellij/openapi/vcs/ex/PartialLocalLineStatusTracker.kt`; IJ-W: `platform/util/diff/src/com/intellij/util/diff/MyersLCS.kt`, `platform/vcs-log/graph/src/com/intellij/vcs/log/graph/utils/GraphUtil.kt`
- `set(IntRange)` (1) — K: `libraries/stdlib/native-wasm/src/kotlin/text/regex/AbstractCharClass.kt`

### Bulk bitwise

| Method | Kotlin | IntelliJ | IntelliJ-J | IntelliJ-W | Total |
|---|---:|---:|---:|---:|---:|
| `or()` | 10 | 11 | 11 | 0 | 21 |
| `andNot()` | 4 | 1 | 1 | 0 | 5 |
| `and()` | 4 | 0 | 0 | 0 | 4 |
| `xor()` | 1 | 0 | 0 | 0 | 1 |

Traceability:
- `or()` (21) — representative files: K: `compiler/backend/src/org/jetbrains/kotlin/codegen/coroutines/resumePointDependentAnalysis.kt`; IJ-J: `java/idea-ui/src/com/intellij/ide/util/importProject/RootDetectionProcessor.java`
- `andNot()` (5) — K: `compiler/ir/backend.common/src/org/jetbrains/kotlin/backend/common/lower/optimizations/LivenessAnalysis.kt`, `kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/optimizations/CastsOptimization.kt`, `kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/optimizations/ComputeTypesPass.kt`, `libraries/stdlib/native-wasm/src/kotlin/text/regex/CharClass.kt`; IJ-J: `platform/analysis-impl/src/com/intellij/codeInspection/dataFlow/lang/ir/LiveVariablesAnalyzer.java`
- `and()` (4) — K: `kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/optimizations/ComputeTypesPass.kt`, `kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/optimizations/DevirtualizationAnalysis.kt`, `kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/optimizations/StaticInitializersOptimization.kt`, `libraries/stdlib/native-wasm/src/kotlin/text/regex/CharClass.kt`
- `xor()` (1) — K: `libraries/stdlib/native-wasm/src/kotlin/text/regex/CharClass.kt`

### Navigation

| Method | Kotlin | IntelliJ | IntelliJ-J | IntelliJ-W | Total |
|---|---:|---:|---:|---:|---:|
| `nextSetBit()` | 3 | 26 | 25 | 1 | 29 |
| `forEachBit()` | 6 | 0 | 0 | 0 | 6 |
| `nextClearBit()` | 1 | 5 | 5 | 0 | 6 |
| `mapEachBit()` | 1 | 0 | 0 | 0 | 1 |

Traceability:
- `nextSetBit()` (29) — representative files: K: `compiler/util/src/org/jetbrains/kotlin/utils/BitSetUtil.kt`; IJ-J: `java/java-impl-refactorings/src/com/intellij/refactoring/extractMethod/ExtractMethodRecommenderInspection.java`; IJ-W: `platform/todo/src/com/intellij/psi/impl/cache/impl/IndexTodoCacheManagerImpl.java`
- `forEachBit()` (6) — K: `compiler/backend/src/org/jetbrains/kotlin/codegen/coroutines/resumePointDependentAnalysis.kt`, `compiler/ir/backend.common/src/org/jetbrains/kotlin/backend/common/lower/optimizations/LivenessAnalysis.kt`, `kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/optimizations/CallGraphBuilder.kt`, `kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/optimizations/CastsOptimization.kt`, `kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/optimizations/ComputeTypesPass.kt`, `kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/optimizations/DevirtualizationAnalysis.kt`
- `nextClearBit()` (6) — K: `libraries/stdlib/native-wasm/src/kotlin/text/regex/AbstractCharClass.kt`; IJ-J: `platform/diff-impl/src/com/intellij/diff/comparison/ComparisonManagerImpl.java`, `platform/diff-impl/src/com/intellij/diff/util/DiffUtil.java`, `platform/diff-impl/src/com/intellij/openapi/vcs/ex/DocumentTracker.kt`, `platform/vcs-impl/src/com/intellij/openapi/vcs/changes/actions/diff/lst/LocalTrackerDiffUtil.kt`, `platform/vcs-impl/src/com/intellij/openapi/vcs/ex/PartialLocalLineStatusTracker.kt`
- `mapEachBit()` (1) — K: `kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/optimizations/ComputeTypesPass.kt`

### Query

| Method | Kotlin | IntelliJ | IntelliJ-J | IntelliJ-W | Total |
|---|---:|---:|---:|---:|---:|
| `isEmpty()` | 3 | 15 | 14 | 1 | 18 |
| `cardinality()` | 3 | 13 | 11 | 2 | 16 |
| `size()` | 3 | 6 | 1 | 5 | 9 |
| `length()` | 1 | 6 | 6 | 0 | 7 |
| `intersects()` | 3 | 0 | 0 | 0 | 3 |

Traceability:
- `isEmpty()` (18) — representative files: K: `analysis/symbol-light-classes/src/org/jetbrains/kotlin/light/classes/symbol/classes/symbolLightClassUtils.kt`; IJ-J: `java/idea-ui/src/com/intellij/ide/util/importProject/RootDetectionProcessor.java`; IJ-W: `platform/syntax/syntax-api/src/com/intellij/platform/syntax/SyntaxElementTypeSet.kt`
- `cardinality()` (16) — representative files: K: `kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/optimizations/CastsOptimization.kt`; IJ-J: `java/java-analysis-impl/src/com/intellij/codeInspection/bytecodeAnalysis/ProjectBytecodeAnalysis.java`; IJ-W: `fleet/util/core/srcCommonMain/fleet/util/BitSet.kt`
- `size()` (9) — K: `compiler/psi/parser/src/org/jetbrains/kotlin/kdoc/lexer/_KDocLexer.java`, `compiler/util/src/org/jetbrains/kotlin/utils/BitSetUtil.kt`, `kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/optimizations/CastsOptimization.kt`; IJ-J: `platform/lang-impl/src/com/intellij/util/indexing/projectFilter/ProjectIndexableFilesFilterHealthCheck.kt`; IJ-W: `platform/lang-impl/src/com/intellij/openapi/roots/impl/FilesScanExecutor.kt`, `platform/lang-impl/src/com/intellij/util/indexing/events/DirtyFiles.kt`, `platform/lang-impl/src/com/intellij/util/indexing/projectFilter/CachingProjectIndexableFilesFilter.kt`, `platform/lang-impl/src/com/intellij/util/indexing/projectFilter/ConcurrentFileIds.kt`, `platform/vcs-log/graph/src/com/intellij/vcs/log/graph/impl/permanent/PermanentLinearGraphBuilder.java`
- `length()` (7) — K: `analysis/symbol-light-classes/src/org/jetbrains/kotlin/light/classes/symbol/classes/symbolLightClassUtils.kt`; IJ-J: `platform/diff-impl/src/com/intellij/openapi/vcs/ex/DocumentTracker.kt`, `platform/vcs-impl/src/com/intellij/openapi/vcs/ex/PartialLocalLineStatusTracker.kt`, `plugins/java-decompiler/engine/src/org/jetbrains/java/decompiler/modules/decompiler/exps/InvocationExprent.java`, `plugins/java-decompiler/engine/src/org/jetbrains/java/decompiler/modules/decompiler/stats/Statement.java`, `plugins/java-decompiler/engine/src/org/jetbrains/java/decompiler/struct/attr/StructLocalVariableTableAttribute.java`, `plugins/java-decompiler/engine/src/org/jetbrains/java/decompiler/util/DebugPrinter.java`
- `intersects()` (3) — K: `analysis/symbol-light-classes/src/org/jetbrains/kotlin/light/classes/symbol/classes/symbolLightClassUtils.kt`, `kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/optimizations/CastsOptimization.kt`, `libraries/stdlib/native-wasm/src/kotlin/text/regex/AbstractCharClass.kt`

### Conversion

| Method | Kotlin | IntelliJ | IntelliJ-J | IntelliJ-W | Total |
|---|---:|---:|---:|---:|---:|
| `clone()` | 2 | 5 | 4 | 1 | 7 |
| `copy()` | 7 | 0 | 0 | 0 | 7 |
| `stream()` | 0 | 6 | 6 | 0 | 6 |
| `toString()` | 1 | 2 | 2 | 0 | 3 |
| `toByteArray()` | 0 | 2 | 2 | 0 | 2 |
| `toLongArray()` | 0 | 1 | 1 | 0 | 1 |

Traceability:
- `clone()` (7) — K: `analysis/symbol-light-classes/src/org/jetbrains/kotlin/light/classes/symbol/classes/symbolLightClassUtils.kt`, `analysis/symbol-light-classes/src/org/jetbrains/kotlin/light/classes/symbol/symbolLightUtils.kt`; IJ-J: `platform/analysis-impl/src/com/intellij/codeInspection/dataFlow/lang/ir/LiveVariablesAnalyzer.java`, `plugins/groovy/groovy-psi/src/org/jetbrains/plugins/groovy/lang/psi/dataFlow/readWrite/ReadBeforeWriteInstance.kt`, `plugins/groovy/groovy-psi/src/org/jetbrains/plugins/groovy/lang/psi/dataFlow/readWrite/ReadBeforeWriteState.kt`, `plugins/groovy/groovy-psi/src/org/jetbrains/plugins/groovy/lang/psi/dataFlow/types/TypeDfaState.java`; IJ-W: `platform/vcs-log/graph/src/com/intellij/vcs/log/graph/collapsing/CollapsedGraph.java`
- `copy()` (7) — K: `analysis/symbol-light-classes/src/org/jetbrains/kotlin/light/classes/symbol/classes/symbolLightClassUtils.kt`, `compiler/backend/src/org/jetbrains/kotlin/codegen/coroutines/resumePointDependentAnalysis.kt`, `compiler/ir/backend.common/src/org/jetbrains/kotlin/backend/common/lower/optimizations/LivenessAnalysis.kt`, `kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/optimizations/CastsOptimization.kt`, `kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/optimizations/ComputeTypesPass.kt`, `kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/optimizations/DevirtualizationAnalysis.kt`, `kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/optimizations/StaticInitializersOptimization.kt`
- `stream()` (6) — IJ-J: `grid/impl/src/run/ui/HiddenColumnsSelectionHolder.java`, `java/java-impl-inspections/src/com/intellij/codeInspection/defUse/DefUseInspection.java`, `platform/analysis-impl/src/com/intellij/codeInspection/dataFlow/lang/ir/LiveVariablesAnalyzer.java`, `platform/analysis-impl/src/com/intellij/codeInspection/dataFlow/memory/DfaMemoryStateImpl.java`, `platform/util/src/com/intellij/util/indexing/containers/BitSetAsRAIntContainer.java`, `platform/vcs-impl/src/com/intellij/openapi/vcs/changes/actions/diff/lst/UnifiedLocalChangeListDiffViewer.kt`
- `toString()` (3) — K: `compiler/backend/src/org/jetbrains/kotlin/codegen/coroutines/resumePointDependentAnalysis.kt`; IJ-J: `platform/analysis-impl/src/com/intellij/codeInspection/dataFlow/rangeSet/LongRangeSet.java`, `plugins/groovy/groovy-psi/src/org/jetbrains/plugins/groovy/lang/psi/dataFlow/readWrite/ReadBeforeWriteState.kt`
- `toByteArray()` (2) — IJ-J: `java/java-analysis-impl/src/com/intellij/codeInspection/dataFlow/inference/MethodDataExternalizer.kt`, `platform/diff-impl/src/com/intellij/openapi/vcs/ex/DocumentTracker.kt`
- `toLongArray()` (1) — IJ-J: `grid/impl/src/run/ui/HiddenColumnsSelectionHolder.java`

### Equality

| Method | Kotlin | IntelliJ | IntelliJ-J | IntelliJ-W | Total |
|---|---:|---:|---:|---:|---:|
| `equals()` | 5 | 4 | 4 | 0 | 9 |
| `hashCode()` | 2 | 1 | 1 | 0 | 3 |

Traceability:
- `equals()` (9) — K: `analysis/symbol-light-classes/src/org/jetbrains/kotlin/light/classes/symbol/classes/symbolLightClassUtils.kt`, `analysis/symbol-light-classes/src/org/jetbrains/kotlin/light/classes/symbol/methods/SymbolLightMethod.kt`, `compiler/backend/src/org/jetbrains/kotlin/codegen/coroutines/resumePointDependentAnalysis.kt`, `compiler/backend/src/org/jetbrains/kotlin/codegen/optimization/common/variableLiveness.kt`, `kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/optimizations/DevirtualizationAnalysis.kt`; IJ-J: `platform/analysis-impl/src/com/intellij/codeInspection/dataFlow/lang/ir/LiveVariablesAnalyzer.java`, `plugins/groovy/groovy-psi/src/org/jetbrains/plugins/groovy/lang/psi/dataFlow/types/TypeDfaState.java`, `plugins/kotlin/idea/src/org/jetbrains/kotlin/idea/refactoring/move/moveDeclarations/ui/MoveKotlinNestedClassesToUpperLevelDialog.java`, `plugins/kotlin/idea/src/org/jetbrains/kotlin/idea/refactoring/move/moveDeclarations/ui/MoveKotlinTopLevelDeclarationsDialog.java`
- `hashCode()` (3) — K: `analysis/symbol-light-classes/src/org/jetbrains/kotlin/light/classes/symbol/classes/symbolLightClassUtils.kt`, `compiler/backend/src/org/jetbrains/kotlin/codegen/optimization/common/variableLiveness.kt`; IJ-J: `platform/analysis-impl/src/com/intellij/codeInspection/dataFlow/lang/ir/LiveVariablesAnalyzer.java`

### Other

| Method | Kotlin | IntelliJ | IntelliJ-J | IntelliJ-W | Total |
|---|---:|---:|---:|---:|---:|
| `clear()` | 2 | 10 | 4 | 6 | 12 |
| `forEachWord()` | 1 | 0 | 0 | 0 | 1 |
| `orWithFilterHasChanged()` | 1 | 0 | 0 | 0 | 1 |

Traceability:
- `clear()` (12) — representative files: K: `compiler/backend/src/org/jetbrains/kotlin/codegen/coroutines/resumePointDependentAnalysis.kt`; IJ-J: `grid/impl/src/run/ui/HiddenColumnsSelectionHolder.java`; IJ-W: `platform/lang-api/src/com/intellij/util/indexing/LightDirectoryIndex.java`
- `forEachWord()` (1) — K: `kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/optimizations/CastsOptimization.kt`
- `orWithFilterHasChanged()` (1) — K: `kotlin-native/backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/optimizations/DevirtualizationAnalysis.kt`

## Pass-Through Notes

- Kotlin pass-through use-site файлы (`2`): `analysis/symbol-light-classes/src/org/jetbrains/kotlin/light/classes/symbol/methods/SymbolLightSimpleMethod.kt`, `analysis/symbol-light-classes/src/org/jetbrains/kotlin/light/classes/symbol/methods/SymbolLightConstructor.kt`.
- IntelliJ TSV pass-through entries (`29`) входят в общее число use-sites, но не вносят вклад в частоты методов.
- `mode=J` (`15`): `java/java-analysis-impl/src/com/intellij/codeInspection/dataFlow/inference/inferenceResults.kt`, `platform/diff-impl/src/com/intellij/diff/merge/MergeThreesideViewerActions.kt`, `platform/diff-impl/src/com/intellij/diff/tools/fragmented/UnifiedDiffViewer.java`, `platform/diff-impl/src/com/intellij/diff/tools/simple/SimpleDiffViewer.java`, `platform/diff-impl/src/com/intellij/diff/tools/simple/SimpleThreesideDiffViewer.java`, `platform/syntax/syntax-api/src/com/intellij/platform/syntax/impl/util/MutableBitSet.kt`, `platform/util/ui/src/com/intellij/util/ui/html/utils.kt`, `platform/vcs-impl/src/com/intellij/openapi/vcs/changes/patch/tool/ApplyPatchViewer.java`, `platform/vcs-impl/src/com/intellij/openapi/vcs/ex/RollbackLineStatusAction.java`, `platform/vcs-log/graph/src/com/intellij/vcs/log/graph/utils/UnsignedBitSet.java`, `platform/vcs-log/graph/src/com/intellij/vcs/log/graph/utils/impl/BitSetFlags.java`, `plugins/java-decompiler/engine/src/org/jetbrains/java/decompiler/modules/decompiler/ConcatenationHelper.java`, `plugins/java-decompiler/engine/src/org/jetbrains/java/decompiler/modules/decompiler/SimplifyExprentsHelper.java`, `plugins/java-decompiler/engine/src/org/jetbrains/java/decompiler/modules/decompiler/stats/CatchStatement.java`, `python/python-psi-api/src/com/jetbrains/python/psi/stubs/PyFileStub.java`.
- `mode=W` (`10`): `platform/lang-impl/src/com/intellij/psi/search/MappedFileTypeIndex.java`, `platform/util/diff/src/com/intellij/util/diff/Diff.kt`, `platform/vcs-log/graph/src/com/intellij/vcs/log/graph/collapsing/CollapsedController.java`, `platform/vcs-log/graph/src/com/intellij/vcs/log/graph/impl/facade/ReachableNodes.kt`, `plugins/git4idea/src/git4idea/history/GitHistoryTraverserImpl.kt`, `plugins/git4idea/src/git4idea/rebase/log/GitCommitEditingActionBase.kt`, `updater/src/mslinks/data/BitSet32.java`, `updater/src/mslinks/data/CNRLinkFlags.java`, `updater/src/mslinks/data/LinkFlags.java`, `updater/src/mslinks/data/LinkInfoFlags.java`.
- `mode=?` (`4`): `platform/util/text-matching/srcJvm/com/intellij/util/text/matching/BitSet.kt`, `API dumps (7 файлов)`, `JDK API version files (4 файла)`, `JDK аннотации (1 файл)`.

## Verification

- Проверка `Kotlin + IntelliJ = Total` выполнена для всех `34` методов: расхождений нет.
- Проверка `IntelliJ-J + IntelliJ-W = IntelliJ` выполнена для всех `34` методов: расхождений нет.
- Непустых `cls=use` строк с `mode=?` в TSV нет, поэтому колонка `IntelliJ` полностью раскладывается на `J` и `W`.
- Контрольный масштаб: `20` Kotlin use-sites + `158` IntelliJ use-site entries = `178`; метод-bearing subset: `18 + 129 = 147`.
