# CHANGELOG

## 1.1 (unreleased)

## 1.0.2 (2016-03-07)

### JS
- Safe calls (`x?.let { it }`) are now inlined

### Tools. J2K
- Protected members used outside of inheritors are converted as public
- Support conversion for annotation constructor calls
- Place comments from the middle of the call to the end
- Drop line breaks between operator arguments (except '+', "-", "&&" and "||")
- Add non-null assertions on call site for non-null parameters
