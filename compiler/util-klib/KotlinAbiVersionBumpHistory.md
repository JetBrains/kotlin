# KotlinAbiVersion bump history

- Bump 1.7.0 -> 1.8.0  
- The bump is caused by incompatible change in KotlinIr.proto  
  `1b6a43ba69a 2022-07-21 Vsevolod Tolstopyatov Update IR serialization to reflect changes in IrSyntheticBodyKind for enum entries`  
  Formally, the bump should have been done in Kotlin 1.8.0, but we did it only in Kotlin 1.9.0 because:
  - We screwed up to do it in time [KT-55808](https://youtrack.jetbrains.com/issue/KT-55808)
  - We got lucky because we had human-readable error anyway, so nobody cared
    [KT-53620](https://youtrack.jetbrains.com/issue/KT-53620)

- Bump 1.6.0 -> 1.7.0  
  `76da9df1021 2022-05-26 Pavel Kunyavskiy Bump klib ABI version`
- The bump is caused by incompatible change in KotlinIr.proto  
  `d809e260cb1 2021-10-25 Roman Artemev [KLIB] Support `DefinitelyNotNull` type in KLIB`

- Bump 1.5.0 -> 1.6.0  
  `3403c464fe0 2021-05-26 Roman Artemev [KLIB] Promote library ABI version`
- The bump is caused by incompatible change in KotlinIr.proto  
  `6cdac22a23a 2021-05-26 Roman Artemev [IR] Introduce new IdSignatures`

- Bump 1.4.2 -> 1.5.0  
  `caee17fddb9 2021-04-08 Dmitriy Dolovov [IR] Bump ABI version due to string serialization format change`
- The bump is caused by string serialization format change in IR  
  `50326f019b7 2021-03-30 Dmitriy Dolovov [IR] Use the proper encoding for string serialization`

- Bump 1.4.1 -> 1.4.2  
  `eea5a9102c4 2020-11-06 Alexander Gorshenev Bump klib abi version to 1.4.2 to reflect absence of serialized fake overrides`
- The bump is caused by stopping serializing overrides. Normally this would be forwards incompatible change and minor version
  should have been bumped, not patch version. But since the code supported the case of fake overrides absence from the beginning,
  only patch version was bumped  
  `cb288d47ea5 2020-11-05 Alexander Gorshenev Don't serialize fake overrides anymore`

- Bump 1.4.0 -> 1.4.1  
  `d7226f49522 2020-04-10 Dmitriy Dolovov KLIB. Promote KLIB ABI version`
- The bump is caused by
  - `101442ad14b 2020-04-03 Dmitriy Dolovov KLIB: Store native targets in manifest`
  - `1b06256650d 2020-04-03 Dmitriy Dolovov KLIB: Add 'native_targets' manifest property`

- Bump 0.26.0 -> 1.4.0  
  `b06a3ea5acb 2020-03-06 Alexander Gorshenev Print out abi version as a full triple`
- There is no clear reasons why the bump was needed. I presume that the author was afraid of version format change. The version
  was bumped up to 1.4.0 but not to 1.0.0 presumably because of `isVersionRequirementTableWrittenCorrectly` (It checks that
  version is at least 1.4) but `isVersionRequirementTableWrittenCorrectly` is invoked only for `BinaryVersion` (and presumably
  only for backend specific metadatas, not for IR) => bump up to 1.0.0 was sufficient. Presumably the author wanted to be extra
  safe.

- ...
