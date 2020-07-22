contract fun bar(s: String?) = [returnsNotNull(), returns() implies (s != null)]

