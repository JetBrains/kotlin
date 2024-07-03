# `KotlinAbiVersion` (KLIB ABI version) change history

##### Bump 1.7.0 -> 1.8.0
- Commit: `770d6a4708055798da2f89bedcab4152e6c3f6ae Bump KotlinAbiVersion for Enum.Entries`
- The bump is caused by incompatible change in `KotlinIr.proto`.
  `1b6a43ba69a1765b65ad6592a380ac49b710f575 Update IR serialization to reflect changes in IrSyntheticBodyKind for enum entries`
  Formally, the bump should have been done in Kotlin 1.8.0, but we did it only in Kotlin 1.9.0 because:
  - We screwed up to do it in time [KT-55808](https://youtrack.jetbrains.com/issue/KT-55808)
  - We got lucky because we had human-readable error anyway, so nobody cared [KT-53620](https://youtrack.jetbrains.com/issue/KT-53620)

##### Bump 1.6.0 -> 1.7.0
- Commit: `76da9df10214f59981e5e54fccaeb967d2f0a528 2022-05-26 Bump klib ABI version`
- The bump is caused by incompatible change in `KotlinIr.proto`.
  `d809e260cb19d48a6abfcddfc65e65dfa567bbfd 2021-10-25 [KLIB] Support DefinitelyNotNull type in KLIB`

##### Bump 1.5.0 -> 1.6.0
- Commit: `3403c464fe0bcebce2d0d476144b811b98ad44c2 2021-05-26 [KLIB] Promote library ABI version`
- The bump is caused by incompatible change in `KotlinIr.proto`.
  `6cdac22a23a7211077e99be01108d775cb6c3a08 2021-05-26 [IR] Introduce new IdSignatures`

##### Bump 1.4.2 -> 1.5.0
- Commit: `caee17fddb9e2e0583fca7b13f1e2d8954487e90 2021-04-08 [IR] Bump ABI version due to string serialization format change`
- The bump is caused by string serialization format change in IR.
  `50326f019b7bd7cad743444bb7436ad7dc439d79 2021-03-30 [IR] Use the proper encoding for string serialization`

##### Bump 1.4.1 -> 1.4.2
- Commit: `eea5a9102c490e16c1486d1378b9448bb34b8525 2020-11-06 Bump klib abi version to 1.4.2 to reflect absence of serialized fake overrides`
- The bump is caused by stopping serializing fake overrides.
  Normally this would be a forward-incompatible change, and only a minor version should have been bumped, not a patch version.
  But since the code supported the case of fake overrides absence from the beginning, only patch version was bumped.
  `cb288d47ea5ea36d6cc8fb40d8a5e5a940808163 2020-11-05 Don't serialize fake overrides anymore`

##### Bump 1.4.0 -> 1.4.1
- Commit: `d7226f49522f7f6db00deccf74b704c3b24d349b 2020-04-10 KLIB. Promote KLIB ABI version`
- The bump is caused by:
  - `101442ad14b3722627f846c30300afa8f41cb642 2020-04-03 KLIB: Store native targets in manifest`
  - `1b06256650d8689a3a075a3a357a17c124d05bd9 2020-04-03 KLIB: Add 'native_targets' manifest property`

##### Bump 0.26.0 -> 1.4.0
- Commit: `b06a3ea5acb205aa32163970268c8df3abc41e46 2020-03-06 Print out abi version as a full triple`
- There are no clear reasons why the bump was actually needed.
  Probably the author was afraid of the version format change and wanted to be extra safe.
  The version was bumped up to 1.4.0, but not to 1.0.0 presumably because of `isVersionRequirementTableWrittenCorrectly`
  (it checks that the version is at least 1.4).
  But `isVersionRequirementTableWrittenCorrectly` is invoked only for `BinaryVersion`
  (and presumably only for backend-specific metadata, not for IR) => bump up to 1.0.0 was sufficient.

##### ...
